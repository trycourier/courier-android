package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.DEFAULT_MAX_PAGINATION_LIMIT
import com.courier.android.Courier.Companion.DEFAULT_MIN_PAGINATION_LIMIT
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.CourierException
import com.courier.android.models.CourierGetInboxMessagesResponse
import com.courier.android.models.CourierInboxData
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.models.toMessageSet
import com.courier.android.socket.InboxSocket
import com.courier.android.socket.InboxSocketManager
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.android.utils.Logger
import com.courier.android.utils.log
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal interface InboxMutationHandler {
    suspend fun onInboxReload(isRefresh: Boolean)
    suspend fun onInboxKilled()
    suspend fun onInboxReset(inbox: CourierInboxData, error: Throwable)
    suspend fun onInboxUpdated(inbox: CourierInboxData)
    suspend fun onInboxItemAdded(index: Int, feed: InboxMessageFeed, message: InboxMessage)
    suspend fun onInboxItemRemove(index: Int, feed: InboxMessageFeed, message: InboxMessage)
    suspend fun onInboxItemUpdated(index: Int, feed: InboxMessageFeed, message: InboxMessage)
    suspend fun onInboxPageFetched(feed: InboxMessageFeed, messageSet: InboxMessageSet)
    suspend fun onInboxMessageReceived(message: InboxMessage)
    suspend fun onInboxEventReceived(event: InboxSocket.MessageEvent)
    suspend fun onInboxError(error: Exception)
    suspend fun onUnreadCountChange(count: Int)
}

internal fun Courier.load(isRefresh: Boolean): Deferred<CourierInboxData?> {
    dataPipe?.cancel()
    dataPipe = getInboxData(isRefresh)
    return dataPipe!!
}

internal fun Courier.getInboxData(isRefresh: Boolean) = coroutineScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {

    try {

        // Notify the handler that inbox reload is starting
        inboxMutationHandler.onInboxReload(isRefresh)

        // Fetch initial inbox data (assuming this is a suspend function)
        val newData = loadInbox(isRefresh)

        // Notify success after data is successfully loaded
        inboxMutationHandler.onInboxUpdated(newData)
        inboxMutationHandler.onUnreadCountChange(newData.unreadCount)

        // Return the fetched data
        return@async newData

    } catch (error: Exception) {

        // Disconnect existing socket on error
        InboxSocketManager.closeSocket()

        // Notify the handler about the error
        inboxMutationHandler.onInboxError(error)

        // Return null in case of an error
        return@async null

    }

}

suspend fun Courier.refreshInbox(): CourierInboxData? {
    return load(isRefresh = true).await()
}

private fun Courier.getFetchParams(isRefresh: Boolean, set: InboxMessageSet?): Pair<Int, String?> {

    // First load
    if (set == null) {
        return Pair(paginationLimit, null)
    }

    // Pull to refresh usually
    if (isRefresh) {
        val min = paginationLimit.coerceAtLeast(set.messages.size)
        val max = min.coerceAtMost(DEFAULT_MAX_PAGINATION_LIMIT)
        return Pair(max, null)
    }

    // Pagination from footer
    return Pair(paginationLimit, set.paginationCursor)

}

private suspend fun Courier.loadInbox(isRefresh: Boolean): CourierInboxData = withContext(Dispatchers.IO) {

    // Check if user is signed in
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    // Get all inbox data and start the websocket
    val result = awaitAll(
        async {
            val (limit, cursor) = getFetchParams(isRefresh, courierInboxData?.feed)
            return@async client?.inbox?.getMessages(
                paginationLimit = limit,
                startCursor = cursor
            )
        },
        async {
            val (limit, cursor) = getFetchParams(isRefresh, courierInboxData?.archived)
            return@async client?.inbox?.getArchivedMessages(
                paginationLimit = limit,
                startCursor = cursor
            )
        },
        async {
            client?.inbox?.getUnreadMessageCount()
        }
    )

    // Get the values
    val feedRes = result[0] as CourierGetInboxMessagesResponse
    val archivedRes = result[1] as CourierGetInboxMessagesResponse
    val unreadCount = result[2] as Int

    // Connect the socket after data is fetched
    connectWebSocket()

    // Return the values
    return@withContext CourierInboxData(
        feed = feedRes.toMessageSet(),
        archived = archivedRes.toMessageSet(),
        unreadCount = unreadCount,
    )

}

private suspend fun Courier.connectWebSocket() {

    InboxSocketManager.closeSocket()

    if (client?.options == null) {
        return
    }

    // Create the socket
    val socket = InboxSocketManager.getSocketInstance(client?.options!!)

    // Listen to the events
    socket.receivedMessage = { message ->
        coroutineScope.launch(Dispatchers.IO) {
            inboxMutationHandler.onInboxMessageReceived(message)
        }
    }

    socket.receivedMessageEvent = { messageEvent ->
        when (messageEvent.event) {
            InboxSocket.EventType.MARK_ALL_READ -> {
//                inbox?.readAllMessages()
//                notifyMessagesChanged()
            }
            InboxSocket.EventType.READ -> {
                messageEvent.messageId?.let { messageId ->
//                    inbox?.readMessage(messageId)
//                    notifyMessagesChanged()
                }
            }
            InboxSocket.EventType.UNREAD -> {
                messageEvent.messageId?.let { messageId ->
//                    inbox?.unreadMessage(messageId)
//                    notifyMessagesChanged()
                }
            }
            InboxSocket.EventType.ARCHIVE -> {
                client?.log("Message Archived")
            }
            InboxSocket.EventType.OPENED -> {
                client?.log("Message Opened")
            }
            else -> {
                client?.log("Unsupported option")
            }
        }
    }

    // Connect the socket
    socket.connect()

    // Subscribe to the events
    socket.sendSubscribe()

}

internal fun Courier.closeInbox() {

//    // Stops all the jobs
//    dataPipe?.cancel()
//    dataPipe = null
//
//    // Close the socket
//    InboxSocketManager.closeSocket()
//
//    // Remove values
//    this.courierInboxData = null
//
//    // Update the listeners
//    notifyError(CourierException.userNotFound)

}

suspend fun Courier.fetchNextInboxPage(feed: InboxMessageFeed): InboxMessageSet? {

    try {

        fetchNextPageOfMessages(feed)?.let {
            inboxMutationHandler.onInboxPageFetched(feed, it)
            return it
        }

    } catch (error: CourierException) {

        inboxMutationHandler.onInboxError(error)

    }

    return null

}

private suspend fun Courier.fetchNextPageOfMessages(feed: InboxMessageFeed): InboxMessageSet? {

    val messageSet = if (feed == InboxMessageFeed.FEED) courierInboxData?.feed else courierInboxData?.archived

    // Determine if we are safe to page
    val canPage = messageSet?.canPaginate == true
    val paginationCursor = messageSet?.paginationCursor
    if (isPagingInbox || !canPage || paginationCursor == null) {
        return null
    }

    if (!Courier.shared.isUserSignedIn) {
        throw CourierException.userNotFound
    }

    if (dataPipe?.isActive == true) {
        return null
    }

    if (courierInboxData == null) {
        throw CourierException.inboxNotInitialized
    }

    isPagingInbox = true

    // Fetch the next page
    val res = when (feed) {
        InboxMessageFeed.FEED -> {
            client?.inbox?.getMessages(
                paginationLimit = paginationLimit,
                startCursor = paginationCursor
            )
        }
        InboxMessageFeed.ARCHIVE -> {
            client?.inbox?.getArchivedMessages(
                paginationLimit = paginationLimit,
                startCursor = paginationCursor
            )
        }
    }

    isPagingInbox = false

    return res?.toMessageSet()

}

fun Courier.addInboxListener(
    onLoading: ((isRefresh: Boolean) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    onUnreadCountChanged: ((Int) -> Unit)? = null,
    onFeedChanged: ((InboxMessageSet) -> Unit)? = null,
    onArchiveChanged: ((InboxMessageSet) -> Unit)? = null,
    onPageAdded: ((InboxMessageFeed, InboxMessageSet) -> Unit)? = null,
    onMessageChanged: ((InboxMessageFeed, Int, InboxMessage) -> Unit)? = null,
    onMessageAdded: ((InboxMessageFeed, Int, InboxMessage) -> Unit)? = null,
    onMessageRemoved: ((InboxMessageFeed, Int, InboxMessage) -> Unit)? = null
): CourierInboxListener {

    // Create a new inbox listener
    val listener = CourierInboxListener(
        onLoading = onLoading,
        onError = onError,
        onUnreadCountChanged = onUnreadCountChanged,
        onFeedChanged = onFeedChanged,
        onArchiveChanged = onArchiveChanged,
        onPageAdded = onPageAdded,
        onMessageChanged = onMessageChanged,
        onMessageAdded = onMessageAdded,
        onMessageRemoved = onMessageRemoved
    )

    // Keep track of listener
    inboxListeners.add(listener)

    // Check for auth
    if (!isUserSignedIn) {
        Logger.warn("User is not signed in. Please call Courier.shared.signIn(...) to setup the inbox listener.")
        listener.onError?.invoke(CourierException.userNotFound)
        return listener
    }

    // Start the listener
    listener.initialize()

    // Start the data pipes
    courierInboxData?.let { data ->
        if (dataPipe?.isCompleted == true) {
            listener.onLoad(data)
            return listener
        }
    }

    // Start the data pipe
    load(isRefresh = false).start()

    return listener

}

fun Courier.removeInboxListener(listener: CourierInboxListener) = coroutineScope.launch(Dispatchers.IO) {

    try {

        // Look for the listener we need to remove
        inboxListeners.removeAll {
            it == listener
        }

    } catch (e: Exception) {

        client?.log(e.toString())

    }

    // Kill the pipes if nothing is listening
    if (inboxListeners.isEmpty()) {
        closeInbox()
    }

}

fun Courier.removeAllListeners() = coroutineScope.launch(Dispatchers.IO) {
    inboxListeners.clear()
    closeInbox()
}

suspend fun Courier.readAllInboxMessages() {

//    if (!isUserSignedIn) {
//        throw CourierException.userNotFound
//    }
//
//    if (inbox == null) {
//        return
//    }
//
//    // Read the messages
//    val original = inbox!!.readAllMessages()
//
//    // Notify
//    notifyMessagesChanged()
//
//    // Perform datasource change in background
//    coroutineScope.launch(Dispatchers.IO) {
//
//        try {
//            client?.inbox?.readAll()
//        } catch (e: Exception) {
//            inbox?.resetReadAll(original)
//            notifyMessagesChanged()
//            notifyError(e)
//        }
//
//    }

}

internal suspend fun Courier.clickMessage(messageId: String) {

//    if (!isUserSignedIn) {
//        throw CourierException.userNotFound
//    }
//
//    // Unwrap message
//    inbox?.messages?.firstOrNull { it.messageId == messageId }?.let { message ->
//
//        // Unwrap tracking id
//        message.clickTrackingId?.let { trackingId ->
//
//            try {
//                client?.inbox?.click(
//                    messageId = messageId,
//                    trackingId = trackingId
//                )
//            } catch (e: Exception) {
//                client?.error(e.toString())
//            }
//
//        }
//
//    }

}

internal suspend fun Courier.readMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    courierInboxData?.updateMessage(
        messageId = messageId,
        event = InboxSocket.EventType.READ,
        client = client,
        handler = inboxMutationHandler
    )

}

internal suspend fun Courier.unreadMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    courierInboxData?.updateMessage(
        messageId = messageId,
        event = InboxSocket.EventType.UNREAD,
        client = client,
        handler = inboxMutationHandler
    )

}

internal suspend fun Courier.archiveMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    courierInboxData?.updateMessage(
        messageId = messageId,
        event = InboxSocket.EventType.ARCHIVE,
        client = client,
        handler = inboxMutationHandler
    )

}

internal suspend fun Courier.openMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    courierInboxData?.updateMessage(
        messageId = messageId,
        event = InboxSocket.EventType.OPENED,
        client = client,
        handler = inboxMutationHandler
    )

}

// Reconnects and refreshes the data
// Called because the websocket may have disconnected or
// new data may have been sent when the user closed their app
internal fun Courier.linkInbox() {
    if (inboxListeners.isNotEmpty()) {
        coroutineScope.launch(Dispatchers.IO) {
            refreshInbox()
        }
    }
}

// Disconnects the websocket
// Helps keep battery usage lower
internal fun Courier.unlinkInbox() {
    if (inboxListeners.isNotEmpty()) {
        coroutineScope.launch(Dispatchers.IO) {
            InboxSocketManager.closeSocket()
        }
    }
}

/**
 * Getters
 */

var Courier.inboxPaginationLimit
    get() = paginationLimit
    set(value) {
        val min = value.coerceAtMost(DEFAULT_MAX_PAGINATION_LIMIT)
        paginationLimit = min.coerceAtLeast(DEFAULT_MIN_PAGINATION_LIMIT)
    }

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

fun Courier.refreshInbox(onComplete: () -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    refreshInbox()
    onComplete.invoke()
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