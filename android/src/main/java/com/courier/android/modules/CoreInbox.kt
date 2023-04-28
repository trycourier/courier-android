package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.*
import com.courier.android.repositories.InboxRepository
import kotlinx.coroutines.*

internal class CoreInbox {

    private var setupPipe: Job? = null
    private var isPaging = false

    private val inboxRepo by lazy { InboxRepository() }
    private var listeners: MutableList<CourierInboxListener> = mutableListOf()
    private var inbox: Inbox? = null

    internal val inboxMessages get() = inbox?.messages

    companion object {
        const val DEFAULT_PAGINATION_LIMIT = 32
        const val DEFAULT_MAX_PAGINATION_LIMIT = 200
        const val DEFAULT_MIN_PAGINATION_LIMIT = 1
    }

    internal var paginationLimit = DEFAULT_PAGINATION_LIMIT

    fun restart() {

        if (listeners.isNotEmpty()) {
            notifyLoading()
            setupPipe = null
            startPipe()
        }

    }

    private suspend fun load(refresh: Boolean = false): Inbox = withContext(Courier.COURIER_COROUTINE_CONTEXT) {

        // Disconnect existing socket
        inboxRepo.disconnectWebsocket()

        // Get all inbox data and start the websocket
        val result = awaitAll(
            async {

                // Determine a safe pagination limit
                val messageCount = this@CoreInbox.inbox?.messages?.size ?: paginationLimit
                val maxRefreshLimit = messageCount.coerceAtMost(DEFAULT_MAX_PAGINATION_LIMIT)
                val limit = if (refresh) maxRefreshLimit else paginationLimit

                // Grab the messages
                return@async inboxRepo.getMessages(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!,
                    paginationLimit = limit
                )

            },
            async {
                inboxRepo.getUnreadMessageCount(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!
                )
            },
            async {
                inboxRepo.connectWebsocket(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!,
                    onMessageReceived = { message ->

                        // Add new message and notify
                        this@CoreInbox.inbox?.addNewMessage(message)
                        notifyMessagesChanged()

                    },
                    onMessageReceivedError = { e ->
                        notifyError(e)
                    }
                )
            }
        )

        // Get the values
        val inboxData = result[0] as InboxData
        val unreadCount = result[1] as Int

        // Return the values
        return@withContext Inbox(
            messages = inboxData.messages?.nodes?.toMutableList(),
            totalCount = inboxData.count ?: 0,
            unreadCount = unreadCount,
            hasNextPage = inboxData.messages?.pageInfo?.hasNextPage,
            startCursor = inboxData.messages?.pageInfo?.startCursor
        )

    }

    suspend fun refresh() {
        try {
            this@CoreInbox.inbox = load(refresh = true)
            notifyMessagesChanged()
        } catch (error: Exception) {
            notifyError(error)
        }
    }

    suspend fun close() {

        // Close the socket
        inboxRepo.disconnectWebsocket()

        // Remove values
        this.inbox = null

        // Update the listeners
        notifyError(CourierException.inboxUserNotFound)

    }

    private fun startPipe() {

        if (setupPipe == null) {

            // Opens the connections and grabs the inbox data
            setupPipe = coroutineScope.launch(Dispatchers.IO) {

                try {

                    // Get the initial data
                    this@CoreInbox.inbox = load()

                    // Notify Success
                    setupPipe?.invokeOnCompletion {
                        notifyMessagesChanged()
                    }

                } catch (error: Exception) {

                    // Notify Error
                    setupPipe?.invokeOnCompletion {
                        notifyError(error)
                    }

                }

            }

        }

    }

    internal suspend fun fetchNextPage(): List<InboxMessage> {

        if (inbox == null) {
            throw CourierException.inboxNotInitialized
        }

        // Determine if we are safe to page
        if (isPaging || this@CoreInbox.inbox?.hasNextPage == false) {
            return emptyList()
        }

        isPaging = true

        var messages = listOf<InboxMessage>()

        try {
            messages = fetchNextPageOfMessages()
            notifyMessagesChanged()
        } catch (error: CourierException) {
            notifyError(error)
        }

        isPaging = false

        return messages

    }

    private suspend fun fetchNextPageOfMessages(): List<InboxMessage> {

        // Check for auth
        if (!Courier.shared.isUserSignedIn || Courier.shared.clientKey == null || this@CoreInbox.inbox == null) {
            throw CourierException.inboxUserNotFound
        }

        // Fetch the next page
        val inboxData = inboxRepo.getMessages(
            clientKey = Courier.shared.clientKey!!,
            userId = Courier.shared.userId!!,
            paginationLimit = paginationLimit,
            startCursor = this@CoreInbox.inbox!!.startCursor
        )

        val messages = inboxData.messages?.nodes.orEmpty()
        val startCursor = inboxData.messages?.pageInfo?.startCursor
        val hasNextPage = inboxData.messages?.pageInfo?.hasNextPage

        // Add the page of messages
        this@CoreInbox.inbox!!.addPage(
            messages = messages,
            startCursor = startCursor,
            hasNextPage = hasNextPage,
        )

        // Return the new messages
        return messages

    }

    internal fun addInboxListener(onInitialLoad: (() -> Unit)? = null, onError: ((Exception) -> Unit)? = null, onMessagesChanged: ((messages: List<InboxMessage>, unreadMessageCount: Int, totalMessageCount: Int, canPaginate: Boolean) -> Unit)? = null): CourierInboxListener {

        // Create a new inbox listener
        val listener = CourierInboxListener(
            onInitialLoad = onInitialLoad,
            onError = onError,
            onMessagesChanged = onMessagesChanged
        )

        // Keep track of listener
        listeners.add(listener)

        // Check for auth
        if (!Courier.shared.isUserSignedIn || Courier.shared.clientKey == null) {
            Courier.log("User is not signed in. Please call Courier.shared.signIn(...) to setup the inbox listener.")
            listener.onError?.invoke(CourierException.inboxUserNotFound)
            return listener
        }

        // Start the listener
        listener.initialize()

        // Start the data pipes
        startPipe()

        // Get the current data if available
        if (setupPipe?.isCompleted == true) {
            listener.notifyMessageChanged()
        }

        return listener

    }

    internal fun removeInboxListener(listener: CourierInboxListener) = coroutineScope.launch(Dispatchers.IO) {

        // Look for the listener we need to remove
        listeners.removeAll {
            it == listener
        }

        // Kill the pipes if nothing is listening
        if (listeners.isEmpty()) {
            close()
        }

    }

    internal fun removeAllListeners() = coroutineScope.launch(Dispatchers.IO) {
        listeners.clear()
        close()
    }

    internal suspend fun readAllMessages() {

        if (!Courier.shared.isUserSignedIn || Courier.shared.clientKey == null) {
            throw CourierException.inboxUserNotFound
        }

        if (this@CoreInbox.inbox == null) {
            return
        }

        // Read the messages
        val original = this@CoreInbox.inbox!!.readAllMessages()

        // Notify
        notifyMessagesChanged()

        // Perform datasource change in background
        coroutineScope.launch(Dispatchers.IO) {

            try {
                inboxRepo.readAllMessages(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!
                )
            } catch (e: Exception) {
                this@CoreInbox.inbox?.resetReadAll(original)
                notifyMessagesChanged()
                notifyError(e)
            }

        }

    }

    internal fun readMessage(messageId: String) {

        if (!Courier.shared.isUserSignedIn || Courier.shared.clientKey == null) {
            throw CourierException.inboxUserNotFound
        }

        if (this@CoreInbox.inbox == null) {
            throw CourierException.inboxNotInitialized
        }

        // Mark the message as read instantly
        val original = this@CoreInbox.inbox!!.readMessage(messageId)

        // Notify
        notifyMessagesChanged()

        // Perform datasource change in background
        coroutineScope.launch(Dispatchers.IO) {

            try {
                inboxRepo.readMessage(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!,
                    messageId = messageId,
                )
            } catch (e: Exception) {
                this@CoreInbox.inbox?.resetUpdate(original)
                notifyMessagesChanged()
                notifyError(e)
            }

        }

    }

    internal fun unreadMessage(messageId: String) {

        if (!Courier.shared.isUserSignedIn || Courier.shared.clientKey == null) {
            throw CourierException.inboxUserNotFound
        }

        if (this@CoreInbox.inbox == null) {
            throw CourierException.inboxNotInitialized
        }

        // Mark the message as read instantly
        val original = this@CoreInbox.inbox!!.unreadMessage(messageId)

        // Notify
        notifyMessagesChanged()

        // Perform datasource change in background
        coroutineScope.launch(Dispatchers.IO) {

            try {
                inboxRepo.unreadMessage(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!,
                    messageId = messageId,
                )
            } catch (e: Exception) {
                this@CoreInbox.inbox?.resetUpdate(original)
                notifyMessagesChanged()
                notifyError(e)
            }

        }

    }

    private fun notifyLoading() {
        listeners.forEach {
            it.onInitialLoad?.invoke()
        }
    }

    private fun notifyMessagesChanged() = coroutineScope.launch(Dispatchers.Main) {
        listeners.forEach {
            it.notifyMessageChanged()
        }
    }

    private fun notifyError(error: Exception) = coroutineScope.launch(Dispatchers.Main) {
        listeners.forEach {
            it.onError?.invoke(error)
        }
    }

    private fun CourierInboxListener.notifyMessageChanged() = coroutineScope.launch(Dispatchers.Main) {
        onMessagesChanged?.invoke(
            inbox?.messages ?: emptyList(),
            inbox?.unreadCount ?: 0,
            inbox?.totalCount ?: 0,
            inbox?.hasNextPage ?: false
        )
    }

}

/**
 * Extensions
 */

val Courier.inboxMessages get() = inbox.inboxMessages

var Courier.inboxPaginationLimit
    get() = inbox.paginationLimit
    set(value) {
        val min = value.coerceAtMost(CoreInbox.DEFAULT_MAX_PAGINATION_LIMIT)
        inbox.paginationLimit = min.coerceAtLeast(CoreInbox.DEFAULT_MIN_PAGINATION_LIMIT)
    }

suspend fun Courier.fetchNextPageOfMessages(): List<InboxMessage> {
    return inbox.fetchNextPage()
}

fun Courier.fetchNextPageOfMessages(onSuccess: (List<InboxMessage>) -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
    try {
        val messages = fetchNextPageOfMessages()
        coroutineScope.launch(Dispatchers.Main) {
            onSuccess.invoke(messages)
        }
    } catch (e: Exception) {
        coroutineScope.launch(Dispatchers.Main) {
            onFailure.invoke(e)
        }
    }
}

/**
 * Creates a CourierInboxListener to watch changes from the Courier Inbox
 **/
fun Courier.addInboxListener(onInitialLoad: (() -> Unit)? = null, onError: ((Exception) -> Unit)? = null, onMessagesChanged: ((messages: List<InboxMessage>, unreadMessageCount: Int, totalMessageCount: Int, canPaginate: Boolean) -> Unit)? = null): CourierInboxListener {
    return inbox.addInboxListener(
        onInitialLoad = onInitialLoad,
        onError = onError,
        onMessagesChanged = onMessagesChanged
    )
}

/**
 * Handle refreshing the data
 */
suspend fun Courier.refreshInbox() {
    inbox.refresh()
}

fun Courier.refreshInbox(onComplete: () -> Unit) = coroutineScope.launch(Dispatchers.IO) {
    refreshInbox()
    onComplete.invoke()
}

/**
 * Removes all the listeners from the Courier Inbox
 **/
fun Courier.removeAllListeners() {
    inbox.removeAllListeners()
}

suspend fun Courier.readAllInboxMessages() {
    inbox.readAllMessages()
}

fun Courier.readAllInboxMessages(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
    try {
        readAllInboxMessages()
        onSuccess.invoke()
    } catch (e: Exception) {
        onFailure.invoke(e)
    }
}

/**
 * Internal Classes
 */

private data class Inbox(
    var messages: MutableList<InboxMessage>?,
    var totalCount: Int,
    var unreadCount: Int,
    var hasNextPage: Boolean?,
    var startCursor: String?
) {

    fun addNewMessage(message: InboxMessage) {
        this.messages?.add(0, message)
        this.totalCount += 1
        this.unreadCount += 1
    }

    fun addPage(messages: List<InboxMessage>, startCursor: String?, hasNextPage: Boolean?) {
        this.messages?.addAll(messages)
        this.startCursor = startCursor
        this.hasNextPage = hasNextPage
    }

    fun readAllMessages(): ReadAllOperation {

        // Copy previous values
        val originalMessages = this.messages?.map { it.copy() }?.toMutableList()
        val originalUnreadCount = this.unreadCount

        // Read all messages
        this.messages?.forEach { it.setRead() }
        this.unreadCount = 0

        return ReadAllOperation(
            messages = originalMessages,
            unreadCount = originalUnreadCount
        )

    }

    // Return the index up the updated message
    fun readMessage(messageId: String): UpdateOperation {

        val messages = messages.orEmpty()
        val index = messages.indexOfFirst { it.messageId == messageId }

        if (index == -1) {
            throw CourierException.inboxMessageNotFound
        }

        // Save copy
        val message = messages[index]
        val originalMessage = message.copy()
        val originalUnreadCount = this.unreadCount

        // Update
        message.setRead()
        this.unreadCount -= 1
        this.unreadCount = this.unreadCount.coerceAtLeast(0)

        return UpdateOperation(
            index = index,
            unreadCount = originalUnreadCount,
            message = originalMessage
        )

    }

    fun unreadMessage(messageId: String): UpdateOperation {

        val messages = messages.orEmpty()
        val index = messages.indexOfFirst { it.messageId == messageId }

        if (index == -1) {
            throw CourierException.inboxMessageNotFound
        }

        // Save copy
        val message = messages[index]
        val originalMessage = message.copy()
        val originalUnreadCount = this.unreadCount

        // Update
        message.setUnread()
        this.unreadCount += 1
        this.unreadCount = this.unreadCount.coerceAtLeast(0)

        return UpdateOperation(
            index = index,
            unreadCount = originalUnreadCount,
            message = originalMessage
        )

    }

    fun resetReadAll(update: ReadAllOperation) {
        this.messages = update.messages
        this.unreadCount = update.unreadCount
    }

    fun resetUpdate(update: UpdateOperation) {
        this.messages?.set(update.index, update.message)
        this.unreadCount = update.unreadCount
    }

}

private data class ReadAllOperation(
    val messages: MutableList<InboxMessage>?,
    val unreadCount: Int,
)

private data class UpdateOperation(
    val index: Int,
    val unreadCount: Int,
    val message: InboxMessage,
)