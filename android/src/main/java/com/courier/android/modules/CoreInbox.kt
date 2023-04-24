package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.*
import com.courier.android.repositories.InboxRepository
import kotlinx.coroutines.*

internal class CoreInbox {

    private data class Inbox(
        var messages: MutableList<InboxMessage>?,
        val totalCount: Int,
        val unreadCount: Int,
        var hasNextPage: Boolean?,
        var startCursor: String?
    )

    private val inboxRepo by lazy { InboxRepository() }

    private var setupPipe: Job? = null

    private var listeners: MutableList<CourierInboxListener> = mutableListOf()

    private var inbox: Inbox? = null

    private suspend fun load() = coroutineScope.launch(Dispatchers.IO) {

        if (Courier.shared.clientKey == null || Courier.shared.userId == null) {
            notifyError(CourierException.inboxUserNotFound)
            return@launch
        }

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
            }
        )

        // Get the values
        val inboxData = result[0] as InboxData
        val unreadCount = result[1] as Int

        // Set the values
        this@CoreInbox.inbox = Inbox(
            messages = inboxData.messages?.nodes?.toMutableList(),
            totalCount = inboxData.count ?: 0,
            unreadCount = unreadCount,
            hasNextPage = inboxData.messages?.pageInfo?.hasNextPage,
            startCursor = inboxData.messages?.pageInfo?.startCursor
        )

    }

    private fun close() {

        // Remove values
        this.inbox = null

        // Update the listeners
        notifyError(CourierException.inboxUserNotFound)

    }

    private fun startPipe(listener: CourierInboxListener) {

        // Start the listener
        listener.initialize()

        // Opens the connections and grabs the inbox data
        setupPipe = setupPipe ?: coroutineScope.launch(Dispatchers.IO) {

            try {

                // Open the pipe
                load()

                // Notify Success
                setupPipe?.invokeOnCompletion {
                    notifyMessagesChanged()
                }

            } catch (error: Exception) {

                // Notify Error
                setupPipe?.invokeOnCompletion {
                    notifyError(error as CourierException)
                }

            }

        }

        // Get the current data if available
        if (setupPipe?.isCompleted == true) {
            listener.notifyMessageChanged()
        }

    }

    internal fun addInboxListener(onInitialLoad: (() -> Unit)? = null, onError: ((CourierException) -> Unit)? = null, onMessagesChanged: ((messages: List<InboxMessage>, unreadMessageCount: Int, totalMessageCount: Int, canPaginate: Boolean) -> Unit)? = null): CourierInboxListener {

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
            notifyError(CourierException.inboxUserNotFound)
            return listener
        }

        // Start the data pipes
        startPipe(listener)

        return listener

    }

    internal fun removeInboxListener(listener: CourierInboxListener) {

        // Look for the listener we need to remove
        listeners.removeAll {
            it == listener
        }

        // Kill the pipes if nothing is listening
        if (listeners.isEmpty()) {
            close()
        }

    }

    internal fun removeAllListeners() {
        listeners.clear()
        close()
    }

    private fun notifyMessagesChanged() {
        listeners.forEach {
            it.notifyMessageChanged()
        }
    }

    private fun notifyError(error: CourierException) {
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

fun Courier.addInboxListener(onInitialLoad: (() -> Unit)? = null, onError: ((CourierException) -> Unit)? = null, onMessagesChanged: ((messages: List<InboxMessage>, unreadMessageCount: Int, totalMessageCount: Int, canPaginate: Boolean) -> Unit)? = null): CourierInboxListener {
    return inbox.addInboxListener(
        onInitialLoad = onInitialLoad,
        onError = onError,
        onMessagesChanged = onMessagesChanged
    )
}