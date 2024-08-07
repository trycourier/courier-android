package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.DEFAULT_MAX_PAGINATION_LIMIT
import com.courier.android.Courier.Companion.DEFAULT_MIN_PAGINATION_LIMIT
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.CourierException
import com.courier.android.models.CourierGetInboxMessagesResponse
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.Inbox
import com.courier.android.models.InboxMessage
import com.courier.android.models.initialize
import com.courier.android.socket.InboxSocket
import com.courier.android.socket.InboxSocketManager
import com.courier.android.utils.Logger
import com.courier.android.utils.log
import com.courier.android.utils.toCourierException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Courier.dataPipeJob
    get() = coroutineScope.launch(start = CoroutineStart.LAZY, context = Dispatchers.IO) {

        try {

            // Get the initial data
            inbox = loadInbox()

            // Notify Success
            dataPipe?.invokeOnCompletion {
                notifyMessagesChanged()
            }

        } catch (error: Exception) {

            // Disconnect existing socket
            InboxSocketManager.closeSocket()

            // Notify Error
            dataPipe?.invokeOnCompletion {
                notifyError(error)
            }

        }

    }

private fun Courier.startDataPipe() {
    dataPipe?.cancel()
    dataPipe = dataPipeJob
    dataPipe?.start()
}

suspend fun Courier.refreshInbox() {
    try {
        inbox = loadInbox(refresh = true)
        notifyMessagesChanged()
    } catch (error: Exception) {
        notifyError(error)
    }
}

private suspend fun Courier.loadInbox(refresh: Boolean = false): Inbox = withContext(Dispatchers.IO) {

    // Check if user is signed in
    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    // Get all inbox data and start the websocket
    val result = awaitAll(
        async {

            // Determine a safe pagination limit
            val currentMessageCount = inbox?.messages?.size ?: paginationLimit
            val minPaginationLimit = currentMessageCount.coerceAtLeast(paginationLimit)
            val maxRefreshLimit = minPaginationLimit.coerceAtMost(DEFAULT_MAX_PAGINATION_LIMIT)
            val limit = if (refresh) maxRefreshLimit else paginationLimit

            // Grab the messages
            return@async client?.inbox?.getMessages(
                paginationLimit = limit
            )

        },
        async {
            client?.inbox?.getUnreadMessageCount()
        },
        async {
            connectWebSocket()
        }
    )

    // Get the values
    val inboxRes = result[0] as CourierGetInboxMessagesResponse
    val unreadCount = result[1] as Int

    // Return the values
    return@withContext Inbox(
        messages = inboxRes.data?.messages?.nodes?.toMutableList(),
        totalCount = inboxRes.data?.count ?: 0,
        unreadCount = unreadCount,
        hasNextPage = inboxRes.data?.messages?.pageInfo?.hasNextPage,
        startCursor = inboxRes.data?.messages?.pageInfo?.startCursor,
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
        inbox?.addNewMessage(message)
        notifyMessagesChanged()
    }

    socket.receivedMessageEvent = { messageEvent ->
        when (messageEvent.event) {
            InboxSocket.EventType.MARK_ALL_READ -> {
                inbox?.readAllMessages()
                notifyMessagesChanged()
            }
            InboxSocket.EventType.READ -> {
                messageEvent.messageId?.let { messageId ->
                    inbox?.readMessage(messageId)
                    notifyMessagesChanged()
                }
            }
            InboxSocket.EventType.UNREAD -> {
                messageEvent.messageId?.let { messageId ->
                    inbox?.unreadMessage(messageId)
                    notifyMessagesChanged()
                }
            }
            InboxSocket.EventType.ARCHIVE -> {
                client?.log("Message Archived")
            }
            InboxSocket.EventType.OPENED -> {
                client?.log("Message Opened")
            }
        }
    }

    // Connect the socket
    socket.connect()

    // Subscribe to the events
    socket.sendSubscribe()

}

internal fun Courier.closeInbox() {

    // Stops all the jobs
    dataPipe?.cancel()
    dataPipe = null

    // Close the socket
    InboxSocketManager.closeSocket()

    // Remove values
    this.inbox = null

    // Update the listeners
    notifyError(CourierException.userNotFound)

}

suspend fun Courier.fetchNextPage(): List<InboxMessage> {

    if (inbox == null) {
        throw CourierException.inboxNotInitialized
    }

    // Determine if we are safe to page
    if (isPaging || inbox?.hasNextPage == false) {
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

private suspend fun Courier.fetchNextPageOfMessages(): List<InboxMessage> {

    // Check for auth
    if (!Courier.shared.isUserSignedIn) {
        throw CourierException.userNotFound
    }

    if (inbox == null) {
        throw CourierException.inboxNotInitialized
    }

    // Fetch the next page
    val inboxData = client?.inbox?.getMessages(
        paginationLimit = paginationLimit,
        startCursor = inbox?.startCursor
    )

    val messages = inboxData?.data?.messages?.nodes.orEmpty()
    val startCursor = inboxData?.data?.messages?.pageInfo?.startCursor
    val hasNextPage = inboxData?.data?.messages?.pageInfo?.hasNextPage

    // Add the page of messages
    inbox?.addPage(
        messages = messages,
        startCursor = startCursor,
        hasNextPage = hasNextPage,
    )

    // Tell the listeners
    notifyMessagesChanged()

    // Return the new messages
    return messages

}

fun Courier.addInboxListener(onInitialLoad: (() -> Unit)? = null, onError: ((Exception) -> Unit)? = null, onMessagesChanged: ((messages: List<InboxMessage>, unreadMessageCount: Int, totalMessageCount: Int, canPaginate: Boolean) -> Unit)? = null): CourierInboxListener {

    // Create a new inbox listener
    val listener = CourierInboxListener(
        onInitialLoad = onInitialLoad,
        onError = onError,
        onMessagesChanged = onMessagesChanged
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
    if (dataPipe?.isCompleted == true) {
        listener.notifyMessagesChanged(inbox)
        return listener
    }

    // Return if the pipe is starting
    if (dataPipe?.isActive == true) {
        return listener
    }

    // Start the data pipes
    startDataPipe()

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

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    if (inbox == null) {
        return
    }

    // Read the messages
    val original = inbox!!.readAllMessages()

    // Notify
    notifyMessagesChanged()

    // Perform datasource change in background
    coroutineScope.launch(Dispatchers.IO) {

        try {
            client?.inbox?.trackAllRead()
        } catch (e: Exception) {
            inbox?.resetReadAll(original)
            notifyMessagesChanged()
            notifyError(e)
        }

    }

}

suspend fun Courier.clickMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    // Unwrap message
    inbox?.messages?.firstOrNull { it.messageId == messageId }?.let { message ->

        // Unwrap tracking id
        message.clickTrackingId?.let { trackingId ->
            client?.inbox?.trackClick(
                messageId = messageId,
                trackingId = trackingId
            )
        }

    }

}

internal suspend fun Courier.readMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    // Mark the message as read instantly
    val original = inbox?.readMessage(messageId)

    // Notify
    notifyMessagesChanged()

    try {

        client?.inbox?.trackRead(
            messageId = messageId,
        )

    } catch (e: Exception) {

        original?.let {
            inbox?.resetUpdate(it)
        }

        notifyMessagesChanged()
        notifyError(e)

    }

}

internal suspend fun Courier.unreadMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    // Mark the message as read instantly
    val original = inbox?.unreadMessage(messageId)

    // Notify
    notifyMessagesChanged()

    try {

        client?.inbox?.trackUnread(
            messageId = messageId,
        )

    } catch (e: Exception) {

        original?.let {
            inbox?.resetUpdate(it)
        }

        notifyMessagesChanged()
        notifyError(e)

    }

}

internal suspend fun Courier.openMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    client?.inbox?.trackOpened(
        messageId = messageId,
    )

}

internal suspend fun Courier.archiveMessage(messageId: String) {

    if (!isUserSignedIn) {
        throw CourierException.userNotFound
    }

    client?.inbox?.trackArchive(
        messageId = messageId,
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

private fun Courier.notifyLoading() = coroutineScope.launch(Dispatchers.Main) {
    inboxListeners.forEach {
        it.onInitialLoad?.invoke()
    }
}

private fun Courier.notifyMessagesChanged() = coroutineScope.launch(Dispatchers.Main) {
    inboxListeners.forEach {
        it.notifyMessagesChanged(inbox)
    }
}

private fun Courier.notifyError(error: Exception) = coroutineScope.launch(Dispatchers.Main) {
    inboxListeners.forEach {
        it.onError?.invoke(error.toCourierException)
    }
}

private fun CourierInboxListener.notifyMessagesChanged(inbox: Inbox?) = coroutineScope.launch(Dispatchers.Main) {
    onMessagesChanged?.invoke(
        inbox?.messages ?: emptyList(),
        inbox?.unreadCount ?: 0,
        inbox?.totalCount ?: 0,
        inbox?.hasNextPage ?: false
    )
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

fun Courier.fetchNextPageOfMessages(onSuccess: ((List<InboxMessage>) -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) = coroutineScope.launch(Dispatchers.Main) {
    try {
        val messages = fetchNextPageOfMessages()
        onSuccess?.invoke(messages)
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