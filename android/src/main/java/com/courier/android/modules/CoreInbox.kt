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
    )

    private var setupPipe: Job? = null
    private val inboxRepo by lazy { InboxRepository() }
    private var listeners: MutableList<CourierInboxListener> = mutableListOf()
    private var inbox: Inbox? = null

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
                inboxRepo.getAllMessages(
                    clientKey = Courier.shared.clientKey!!,
                    userId = Courier.shared.userId!!
                    // TODO: Pagination
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

                        // Update local values
                        this@CoreInbox.inbox?.let { inbox ->
                            inbox.messages?.add(0, message)
                            inbox.totalCount += 1
                            inbox.unreadCount += 1
                        }

                        // Pass the change
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

    private fun notifyMessagesChanged() {
        listeners.forEach {
            it.notifyMessageChanged()
        }
    }

    private fun notifyError(error: Exception) {
        listeners.forEach {
            it.onError?.invoke(error)
        }
    }

    private fun CourierInboxListener.notifyMessageChanged() {
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