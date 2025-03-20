package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.socket.InboxSocket
import com.courier.android.ui.inbox.InboxMessageEvent
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.android.utils.log
import com.courier.android.utils.warn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class InboxModule(private val courier: Courier) : InboxDataStoreEventDelegate {

    enum class Pagination(val value: Int) {
        DEFAULT(32),
        MAX(100),
        MIN(1)
    }

    enum class State {
        UNINITIALIZED,
        FETCHING,
        INITIALIZED
    }

    internal val dataStore = InboxDataStore()
    internal val dataService = InboxDataService()
    internal var state: State = State.UNINITIALIZED
    var paginationLimit: Int = Pagination.DEFAULT.value
        internal set

    init {
        dataStore.delegate = this
    }

    internal fun getTrimmedPaginationLimit(limit: Int): Int {
        return limit.coerceIn(Pagination.MIN.value, Pagination.MAX.value)
    }

    /** Fetching */
    private fun getInitialLimit(messageCount: Int?, isRefresh: Boolean): Int {
        return if (isRefresh) {
            val existingCount = messageCount ?: paginationLimit
            maxOf(existingCount, paginationLimit)
        } else {
            paginationLimit
        }
    }

    suspend fun getInbox(isRefresh: Boolean) {
        try {
            state = State.UNINITIALIZED
            dataService.stop()

            if (inboxListeners.isEmpty()) throw CourierException.inboxNotInitialized
            if (!courier.isUserSignedIn) throw CourierException.userNotFound
            val client = courier.client ?: throw CourierException.inboxNotInitialized

            state = State.FETCHING
            dataStore.delegate?.onLoading(isRefresh)

            val feedPaginationLimit = getInitialLimit(dataStore.feed.messages.size, isRefresh)
            val archivePaginationLimit = getInitialLimit(dataStore.archive.messages.size, isRefresh)

            val snapshot = dataService.getInboxData(client, feedPaginationLimit, archivePaginationLimit, isRefresh)

            dataService.connectWebSocket(
                client = client,
                onReceivedMessage = { message ->
                    CoroutineScope(Dispatchers.IO).launch {
                        dataStore.addMessage(message, 0, InboxMessageFeed.FEED)
                    }
                },
                onReceivedMessageEvent = { event ->
                    CoroutineScope(Dispatchers.IO).launch {
                        handleMessageEvent(event)
                    }
                }
            )

            dataStore.reloadSnapshot(snapshot)
            state = State.INITIALIZED
        } catch (error: Exception) {
            dataStore.delegate?.onError(error)
            state = State.UNINITIALIZED
        }
    }

    private suspend fun handleMessageEvent(event: InboxSocket.MessageEvent) {
        when (event.event) {
            InboxSocket.EventType.MARK_ALL_READ -> dataStore.readAllMessages(null)
            InboxSocket.EventType.READ -> event.messageId?.let {
                val message = InboxMessage(it)
                dataStore.readMessage(message, InboxMessageFeed.FEED, null)
                dataStore.readMessage(message, InboxMessageFeed.ARCHIVE, null)
            }
            InboxSocket.EventType.UNREAD -> event.messageId?.let {
                val message = InboxMessage(it)
                dataStore.unreadMessage(message, InboxMessageFeed.FEED, null)
                dataStore.unreadMessage(message, InboxMessageFeed.ARCHIVE, null)
            }
            InboxSocket.EventType.OPENED -> event.messageId?.let {
                val message = InboxMessage(it)
                dataStore.openMessage(message, InboxMessageFeed.FEED, null)
                dataStore.openMessage(message, InboxMessageFeed.ARCHIVE, null)
            }
            InboxSocket.EventType.ARCHIVE -> event.messageId?.let {
                val message = InboxMessage(it)
                dataStore.archiveMessage(message, InboxMessageFeed.FEED, null)
                dataStore.archiveMessage(message, InboxMessageFeed.ARCHIVE, null)
            }
            else -> courier.client?.warn("Unsupported event type")
        }
    }

    suspend fun getNextPage(feedType: InboxMessageFeed): InboxMessageSet? {
        val isFeed = feedType == InboxMessageFeed.FEED
        val isPaginating = if (isFeed) dataService.isPagingFeed else dataService.isPagingArchived

        if (inboxListeners.isEmpty() || !courier.isUserSignedIn || isPaginating) return null
        val client = courier.client ?: return null

        val limit = paginationLimit
        val messageSet = if (isFeed) dataStore.feed else dataStore.archive

        if (!messageSet.canPaginate) return null
        val cursor = messageSet.paginationCursor ?: return null

        val data = if (isFeed) dataService.getNextFeedPage(client, limit, cursor) else dataService.getNextArchivePage(client, limit, cursor)
        dataStore.addPage(data, feedType)

        return data
    }

    suspend fun kill() {
        state = State.UNINITIALIZED
        dataStore.dispose()
        dataService.stop()
        dataStore.delegate?.onError(CourierException.userNotFound)
    }

    suspend fun dispose() {
        removeAllListeners()
        kill()
    }

    /** Listeners */
    internal val inboxListeners: MutableList<CourierInboxListener> = mutableListOf()

    fun addListener(listener: CourierInboxListener) {
        inboxListeners.add(listener)
        courier.client?.log("Courier Inbox Listener Registered. Total Listeners: ${inboxListeners.size}")
    }

    fun removeListener(listener: CourierInboxListener) {
        inboxListeners.remove(listener)
        courier.client?.log("Courier Inbox Listener Unregistered. Total Listeners: ${inboxListeners.size}")
    }

    fun removeAllListeners() {
        inboxListeners.clear()
        courier.client?.log("Courier Inbox Listeners Removed. Total Listeners: ${inboxListeners.size}")
        dataService.stop()
    }

    /** DataStore Events */
    override suspend fun onLoading(isRefresh: Boolean) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onLoading?.invoke(isRefresh) }
        }
    }

    override suspend fun onError(error: Throwable) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onError?.invoke(error) }
        }
    }

    override suspend fun onMessagesChanged(messages: List<InboxMessage>, canPaginate: Boolean, feed: InboxMessageFeed) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onMessagesChanged?.invoke(messages, canPaginate, feed) }
        }
    }

    override suspend fun onMessageEvent(message: InboxMessage, index: Int, feed: InboxMessageFeed, event: InboxMessageEvent) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onMessageEvent?.invoke(message, index, feed, event) }
        }
    }

    override suspend fun onTotalCountUpdated(totalCount: Int, feed: InboxMessageFeed) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onTotalCountChanged?.invoke(totalCount, feed) }
        }
    }

    override suspend fun onUnreadCountUpdated(unreadCount: Int) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onUnreadCountChanged?.invoke(unreadCount) }
        }
    }

    override suspend fun onPageAdded(messages: List<InboxMessage>, canPaginate: Boolean, isFirstPage: Boolean, feed: InboxMessageFeed) {
        withContext(Dispatchers.Main) {
            inboxListeners.forEach { it.onPageAdded?.invoke(messages, canPaginate, isFirstPage, feed) }
        }
    }
}

suspend fun Courier.fetchNextInboxPage(feed: InboxMessageFeed): InboxMessageSet? {
    return inboxModule.getNextPage(feed)
}

suspend fun Courier.addInboxListener(
    onLoading: ((isRefresh: Boolean) -> Unit)? = null,
    onError: ((error: Throwable) -> Unit)? = null,
    onUnreadCountChanged: ((unreadCount: Int) -> Unit)? = null,
    onTotalCountChanged: ((totalCount: Int, feed: InboxMessageFeed) -> Unit)? = null,
    onMessagesChanged: ((messages: List<InboxMessage>, canPaginate: Boolean, feed: InboxMessageFeed) -> Unit)? = null,
    onPageAdded: ((messages: List<InboxMessage>, canPaginate: Boolean, isFirstPage: Boolean, feed: InboxMessageFeed) -> Unit)? = null,
    onMessageEvent: ((message: InboxMessage, index: Int, feed: InboxMessageFeed, event: InboxMessageEvent) -> Unit)? = null
): CourierInboxListener {

    val listener = CourierInboxListener(
        onLoading, onError, onUnreadCountChanged, onTotalCountChanged,
        onMessagesChanged, onPageAdded, onMessageEvent
    )

    listener.initialize()

    inboxModule.addListener(listener)

    if (!isUserSignedIn) {
        client?.warn("User not signed in. Please call signIn(...) to setup the inbox listener.")
        listener.onError?.invoke(CourierException.userNotFound)
    }

    when (inboxModule.state) {
        InboxModule.State.UNINITIALIZED -> {
            // Get the inbox data
            // If an existing call is going out, it will cancel that call.
            // This will return data for the last inbox listener that is registered
            inboxModule.getInbox(isRefresh = false)
        }
        InboxModule.State.FETCHING -> {
            // Do not hit any callbacks while data is fetching
        }
        InboxModule.State.INITIALIZED -> {
            listener.onLoad(inboxModule.dataStore.getSnapshot())
        }
    }

    return listener

}

suspend fun Courier.removeAllInboxListeners() {
    inboxModule.removeAllListeners()
}

suspend fun Courier.removeInboxListener(listener: CourierInboxListener) {
    inboxModule.removeListener(listener)

    if (inboxModule.inboxListeners.isEmpty()) {
        closeInbox()
    }
}

suspend fun Courier.clickMessage(messageId: String) {
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    val message = InboxMessage(messageId)
    inboxModule.dataStore.clickMessage(message, InboxMessageFeed.FEED, client)
    inboxModule.dataStore.clickMessage(message, InboxMessageFeed.ARCHIVE, client)
}

suspend fun Courier.readMessage(messageId: String) {
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    val client = client ?: throw CourierException.inboxNotInitialized

    val message = InboxMessage(messageId)
    inboxModule.dataStore.readMessage(message, InboxMessageFeed.FEED, client)
    inboxModule.dataStore.readMessage(message, InboxMessageFeed.ARCHIVE, client)
}

suspend fun Courier.unreadMessage(messageId: String) {
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    val client = client ?: throw CourierException.inboxNotInitialized

    val message = InboxMessage(messageId)
    inboxModule.dataStore.unreadMessage(message, InboxMessageFeed.FEED, client)
    inboxModule.dataStore.unreadMessage(message, InboxMessageFeed.ARCHIVE, client)
}

suspend fun Courier.archiveMessage(messageId: String) {
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    val client = client ?: throw CourierException.inboxNotInitialized

    val message = InboxMessage(messageId)
    inboxModule.dataStore.archiveMessage(message, InboxMessageFeed.FEED, client)
}

suspend fun Courier.openMessage(messageId: String) {
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    val client = client ?: throw CourierException.inboxNotInitialized

    val message = InboxMessage(messageId)
    inboxModule.dataStore.openMessage(message, InboxMessageFeed.FEED, client)
    inboxModule.dataStore.openMessage(message, InboxMessageFeed.ARCHIVE, client)
}

suspend fun Courier.readAllInboxMessages() {
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    val client = client ?: throw CourierException.inboxNotInitialized

    inboxModule.dataStore.readAllMessages(client)
}

// Reconnects and refreshes the data
// Called because the websocket may have disconnected or
// new data may have been sent when the user closed their app
internal suspend fun Courier.linkInbox() {
    // Get the socket connection
    val sharedSocket = inboxModule.dataService.inboxSocketManager.socket
    val isSocketConnected = sharedSocket?.isConnected ?: false

    // Only restart if the socket is not connected
    if (!isSocketConnected) {
        refreshInbox()
    }
}

// Disconnects the websocket
// Helps keep battery usage lower
internal fun Courier.unlinkInbox() {
    inboxModule.dataService.stop()
}

suspend fun Courier.restartInbox() {
    if (inboxModule.inboxListeners.isNotEmpty()) {
        inboxModule.getInbox(false)
    }
}

suspend fun Courier.refreshInbox() {
    if (inboxModule.inboxListeners.isNotEmpty()) {
        inboxModule.getInbox(true)
    }
}

fun Courier.refreshInbox(onComplete: () -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    Courier.shared.refreshInbox()
    onComplete.invoke()
}

suspend fun Courier.closeInbox() {
    inboxModule.kill()
}

/**
 * Getters
 */

var Courier.inboxPaginationLimit
    get() = inboxModule.paginationLimit
    set(value) {
        inboxModule.paginationLimit = inboxModule.getTrimmedPaginationLimit(value)
    }

val Courier.feedMessages: List<InboxMessage> get() = inboxModule.dataStore.feed.messages
val Courier.archivedMessages: List<InboxMessage> get() = inboxModule.dataStore.archive.messages

/**
 * Traditional Callbacks
 */

fun Courier.fetchNextInboxPage(feed: InboxMessageFeed, onSuccess: ((InboxMessageSet?) -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        val set = fetchNextInboxPage(feed)
        onSuccess?.invoke(set)
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}

fun Courier.readAllInboxMessages(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    try {
        readAllInboxMessages()
        onSuccess.invoke()
    } catch (e: Exception) {
        onFailure.invoke(e)
    }
}

fun Courier.clickMessage(messageId: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        clickMessage(messageId)
        onSuccess?.invoke()
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}

fun Courier.readMessage(messageId: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        readMessage(messageId)
        onSuccess?.invoke()
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}

fun Courier.unreadMessage(messageId: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        unreadMessage(messageId)
        onSuccess?.invoke()
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}

fun Courier.openMessage(messageId: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        openMessage(messageId)
        onSuccess?.invoke()
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}

fun Courier.archiveMessage(messageId: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        archiveMessage(messageId)
        onSuccess?.invoke()
    } catch (e: Exception) {
        onFailure?.invoke(e)
    }
}