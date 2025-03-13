package com.courier.android.modules

import com.courier.android.client.CourierClient
import com.courier.android.models.CourierException
import com.courier.android.models.CourierGetInboxMessagesResponse
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.models.toMessageSet
import com.courier.android.socket.InboxSocket
import com.courier.android.socket.InboxSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class InboxDataService {

    internal val inboxSocketManager = InboxSocketManager()
    internal var isPagingFeed = false
    internal var isPagingArchived = false

    /** Ends any ongoing pagination processes */
    fun endPaging() {
        isPagingFeed = false
        isPagingArchived = false
    }

    /** Stops any ongoing operations and disconnects the WebSocket */
    fun stop() {
        endPaging()
        inboxSocketManager.closeSocket()
    }

    /**
     * Fetches inbox data: messages for both feed and archive, as well as unread count.
     */
    suspend fun getInboxData(
        client: CourierClient,
        feedPaginationLimit: Int,
        archivePaginationLimit: Int,
        isRefresh: Boolean
    ): Triple<InboxMessageSet, InboxMessageSet, Int> = coroutineScope {
        try {
            val results = awaitAll(
                async {
                    client.inbox.getMessages(
                        paginationLimit = feedPaginationLimit,
                        startCursor = null
                    )
                },
                async {
                    client.inbox.getArchivedMessages(
                        paginationLimit = archivePaginationLimit,
                        startCursor = null
                    )
                },
                async {
                    client.inbox.getUnreadMessageCount()
                }
            )

            val feedRes = results[0] as CourierGetInboxMessagesResponse
            val archivedRes = results[1] as CourierGetInboxMessagesResponse
            val unreadCount = results[2] as Int

            Triple(feedRes.toMessageSet(), archivedRes.toMessageSet(), unreadCount)
        } catch (e: Exception) {
            throw CourierException.inboxNotInitialized
        }
    }

    /**
     * Establishes a WebSocket connection for real-time updates on inbox events.
     */
    suspend fun connectWebSocket(
        client: CourierClient,
        onReceivedMessage: (InboxMessage) -> Unit,
        onReceivedMessageEvent: (InboxSocket.MessageEvent) -> Unit
    ) {
        val socket = inboxSocketManager.updateInstance(client.options)

        // Connect socket listeners
        socket.receivedMessage = { message ->
            CoroutineScope(Dispatchers.IO).launch {
                onReceivedMessage(message)
            }
        }

        socket.receivedMessageEvent = { event ->
            CoroutineScope(Dispatchers.IO).launch {
                onReceivedMessageEvent(event)
            }
        }

        // Connect, subscribe, and keep the socket alive
        socket.connect()
        socket.sendSubscribe()
        socket.keepAlive()
    }

    /**
     * Fetches the next page of messages for the feed.
     */
    suspend fun getNextFeedPage(client: CourierClient, paginationLimit: Int, paginationCursor: String): InboxMessageSet {
        isPagingFeed = true
        return try {
            client.inbox.getMessages(
                paginationLimit = paginationLimit,
                startCursor = paginationCursor
            ).toMessageSet()
        } finally {
            isPagingFeed = false
        }
    }

    /**
     * Fetches the next page of messages for the archive.
     */
    suspend fun getNextArchivePage(client: CourierClient, paginationLimit: Int, paginationCursor: String): InboxMessageSet {
        isPagingArchived = true
        return try {
            client.inbox.getArchivedMessages(
                paginationLimit = paginationLimit,
                startCursor = paginationCursor
            ).toMessageSet()
        } finally {
            isPagingArchived = false
        }
    }
}