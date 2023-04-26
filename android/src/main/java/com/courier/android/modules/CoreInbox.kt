package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.*
import com.courier.android.repositories.InboxRepository
import kotlinx.coroutines.*

internal class CoreInbox {

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

    }

    private var setupPipe: Job? = null
    private var isPaging = false

    private val inboxRepo by lazy { InboxRepository() }
    private var listeners: MutableList<CourierInboxListener> = mutableListOf()
    private var inbox: Inbox? = null

    internal val inboxMessages get() = inbox?.messages

    fun restart() {

        if (listeners.isNotEmpty()) {
            notifyLoading()
            setupPipe = null
            startPipe()
        }

    }

    private suspend fun load(): Inbox = withContext(Courier.COURIER_COROUTINE_CONTEXT) {

        // Disconnect existing socket
        inboxRepo.disconnectWebsocket()

        // Get all inbox data and start the websocket
        val result = awaitAll(
            async {
                inboxRepo.getMessages(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!,
                    paginationLimit = 24 // TODO
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

                } catch (error: CourierException) {

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
            paginationLimit = 24, // TODO,
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

suspend fun Courier.fetchNextPageOfMessages(): List<InboxMessage> {
    return inbox.fetchNextPage()
}

fun Courier.fetchNextPageOfMessages(onSuccess: ((List<InboxMessage>) -> Unit)? = null, onFailure: ((Exception) -> Void)? = null) = coroutineScope.launch(Dispatchers.IO) {
    try {
        val messages = fetchNextPageOfMessages()
        coroutineScope.launch(Dispatchers.Main) {
            onSuccess?.invoke(messages)
        }
    } catch (e: Exception) {
        coroutineScope.launch(Dispatchers.Main) {
            onFailure?.invoke(e)
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
 * Removes all the listeners from the Courier Inbox
 **/
fun Courier.removeAllListeners() {
    inbox.removeAllListeners()
}