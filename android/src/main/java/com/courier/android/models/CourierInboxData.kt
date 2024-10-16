package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.modules.InboxMutationHandler
import com.courier.android.socket.InboxSocket
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.android.utils.log

class CourierInboxData(
    feed: InboxMessageSet,
    archived: InboxMessageSet,
    unreadCount: Int,
) {

    var feed: InboxMessageSet = feed
        internal set
    var archived: InboxMessageSet = archived
        internal set
    var unreadCount: Int = unreadCount
        internal set

    private fun copy(): CourierInboxData {
        return CourierInboxData(
            feed = this.feed,
            archived = this.archived,
            unreadCount = this.unreadCount
        )
    }

    @Synchronized
    internal fun addPage(feed: InboxMessageFeed, messageSet: InboxMessageSet) {
        when (feed) {
            InboxMessageFeed.FEED -> {
                this.feed.messages.addAll(messageSet.messages)
                this.feed.totalCount = messageSet.totalCount
                this.feed.canPaginate = messageSet.canPaginate
                this.feed.paginationCursor = messageSet.paginationCursor
            }
            InboxMessageFeed.ARCHIVE -> {
                this.archived.messages.addAll(messageSet.messages)
                this.archived.totalCount = messageSet.totalCount
                this.archived.canPaginate = messageSet.canPaginate
                this.archived.paginationCursor = messageSet.paginationCursor
            }
        }
    }

    @Synchronized
    internal fun addNewMessage(feed: InboxMessageFeed, index: Int, message: InboxMessage): Int {
        this.unreadCount += 1
        when (feed) {
            InboxMessageFeed.FEED -> {
                this.feed.messages.add(index, message)
                this.feed.totalCount += 1
            }
            InboxMessageFeed.ARCHIVE -> {
                this.archived.messages.add(index, message)
                this.archived.totalCount += 1
            }
        }
        return this.unreadCount
    }

    @Synchronized
    private fun getMessages(messageId: String): Triple<InboxMessageFeed, List<InboxMessage>, Int>? {

        // Check if the message is in the feed
        feed.messages.indexOfFirst { it.messageId == messageId }.takeIf { it >= 0 }?.let { index ->
            return Triple(InboxMessageFeed.FEED, feed.messages, index)
        }

        // Check if the message is in the archived feed
        archived.messages.indexOfFirst { it.messageId == messageId }.takeIf { it >= 0 }?.let { index ->
            return Triple(InboxMessageFeed.ARCHIVE, archived.messages, index)
        }

        // If the message is not found, return null
        return null

    }

    internal suspend fun updateMessage(
        messageId: String,
        event: InboxSocket.EventType,
        client: CourierClient?,
        handler: InboxMutationHandler
    ) {

        client ?: throw CourierException.inboxNotInitialized

        val (feed, messages, index) = getMessages(messageId) ?: return

        // Copy the original state of the data
        val original = copy()
        val originalMessage = messages[index].copy()

        // Change the local data
        mutateLocalData(
            event = event,
            inboxFeed = feed,
            index = index,
            handler = handler
        )

        // Perform server update
        // If fails, reset the change to the original copy
        try {
            mutateServerData(
                client = client,
                message = originalMessage,
                event = event
            )
        } catch (e: Exception) {
            client.options.log(e.localizedMessage ?: "Error occurred")
            handler.onInboxReset(original, e)
        }
    }

    private suspend fun mutateLocalData(
        event: InboxSocket.EventType,
        inboxFeed: InboxMessageFeed,
        index: Int,
        handler: InboxMutationHandler
    ) {
        when (event) {
            InboxSocket.EventType.READ -> read(index, inboxFeed, handler)
            InboxSocket.EventType.UNREAD -> unread(index, inboxFeed, handler)
            InboxSocket.EventType.OPENED -> open(index, inboxFeed, handler)
            InboxSocket.EventType.UNOPENED -> unopen(index, inboxFeed, handler)
            InboxSocket.EventType.ARCHIVE -> archive(index, inboxFeed, handler)
            else -> { /* No action */ }
        }
    }

    private suspend fun mutateServerData(
        client: CourierClient,
        message: InboxMessage,
        event: InboxSocket.EventType
    ) {
        when (event) {
            InboxSocket.EventType.READ -> client.inbox.read(messageId = message.messageId)
            InboxSocket.EventType.UNREAD -> client.inbox.unread(messageId = message.messageId)
            InboxSocket.EventType.OPENED -> client.inbox.open(messageId = message.messageId)
            InboxSocket.EventType.UNOPENED -> { /* No action for unopened */ }
            InboxSocket.EventType.ARCHIVE -> client.inbox.archive(messageId = message.messageId)
            InboxSocket.EventType.UNARCHIVE -> { /* No action for unarchive */ }
            InboxSocket.EventType.CLICK -> {
                message.trackingIds?.clickTrackingId?.let { trackingId ->
                    client.inbox.click(messageId = message.messageId, trackingId = trackingId)
                }
            }
            InboxSocket.EventType.UNCLICK, InboxSocket.EventType.MARK_ALL_READ -> {
                return
            }
        }
    }

    internal suspend fun read(
        index: Int,
        inboxFeed: InboxMessageFeed,
        handler: InboxMutationHandler
    ) {

        when (inboxFeed) {
            InboxMessageFeed.FEED -> {

                if (feed.messages[index].isArchived) {
                    return
                }

                if (!feed.messages[index].isRead) {
                    feed.messages[index].setRead()
                    handler.onInboxItemUpdated(index, inboxFeed, feed.messages[index])
                    unreadCount = maxOf(unreadCount - 1, 0)
                    handler.onUnreadCountChange(unreadCount)
                }

            }
            InboxMessageFeed.ARCHIVE -> {
                // Empty
            }
        }

    }

    internal suspend fun unread(
        index: Int,
        inboxFeed: InboxMessageFeed,
        handler: InboxMutationHandler
    ) {
        when (inboxFeed) {
            InboxMessageFeed.FEED -> {

                if (feed.messages[index].isArchived) {
                    return
                }

                if (feed.messages[index].isRead) {
                    feed.messages[index].setUnread()
                    handler.onInboxItemUpdated(index, inboxFeed, feed.messages[index])
                    unreadCount += 1
                    handler.onUnreadCountChange(unreadCount)
                }

            }
            InboxMessageFeed.ARCHIVE -> {
                // Empty
            }
        }
    }

    internal suspend fun open(
        index: Int,
        inboxFeed: InboxMessageFeed,
        handler: InboxMutationHandler
    ) {
        when (inboxFeed) {
            InboxMessageFeed.FEED -> {
                if (!feed.messages[index].isOpened) {
                    feed.messages[index].setOpened()
                    handler.onInboxItemUpdated(index, inboxFeed, feed.messages[index])
                }
            }
            InboxMessageFeed.ARCHIVE -> {
                // Empty
            }
        }
    }

    private suspend fun unopen(
        index: Int,
        inboxFeed: InboxMessageFeed,
        handler: InboxMutationHandler
    ) {
        when (inboxFeed) {
            InboxMessageFeed.FEED -> {
                if (feed.messages[index].isOpened) {
                    feed.messages[index].setUnopened()
                    handler.onInboxItemUpdated(index, inboxFeed, feed.messages[index])
                }
            }
            InboxMessageFeed.ARCHIVE -> {
                // Empty
            }
        }
    }

    internal suspend fun archive(
        index: Int,
        inboxFeed: InboxMessageFeed,
        handler: InboxMutationHandler
    ) {
        when (inboxFeed) {
            InboxMessageFeed.FEED -> {
                if (!feed.messages[index].isArchived) {
                    // Read the message
                    read(index, inboxFeed, handler)

                    // Change archived status
                    feed.messages[index].setArchived()
                    handler.onInboxItemUpdated(index, inboxFeed, feed.messages[index])

                    // Create copy
                    val newMessage = feed.messages[index].copy()

                    // Remove the item from the feed
                    feed.messages.removeAt(index)
                    handler.onInboxItemRemove(index, InboxMessageFeed.FEED, newMessage)

                    // Add the item to the archive
                    val insertIndex = findInsertIndex(newMessage, archived.messages)
                    insertIndex?.let {
                        archived.messages.add(it, newMessage)
                        handler.onInboxItemAdded(it, InboxMessageFeed.ARCHIVE, newMessage)
                    }
                }
            }
            InboxMessageFeed.ARCHIVE -> {
                // Empty
            }
        }
    }

    private fun findInsertIndex(newMessage: InboxMessage, messages: List<InboxMessage>): Int? {
        return messages.indexOfFirst { newMessage.createdAt >= it.createdAt }.takeIf { it >= 0 }
    }

    internal suspend fun readAllLocalMessages(handler: InboxMutationHandler) {
        feed.messages.forEach { it.setArchived() }
        archived.messages.forEach { it.setArchived() }
        unreadCount = 0
        handler.onInboxUpdated(this)
    }

    internal suspend fun readAll(handler: InboxMutationHandler) {

        val client = Courier.shared.client ?: throw CourierException.inboxNotInitialized

        val original = copy()

        // Perform the local change
        readAllLocalMessages(handler)

        // Perform server update
        try {
            client.inbox.readAll()
        } catch (e: Exception) {
            client.options.log(e.localizedMessage ?: "Error occurred")
            handler.onInboxReset(original, e)
        }

    }

}

class InboxMessageSet(
    messages: MutableList<InboxMessage>,
    totalCount: Int,
    canPaginate: Boolean,
    paginationCursor: String?
) {

    var messages: MutableList<InboxMessage> = messages
        internal set
    var totalCount: Int = totalCount
        internal set
    var canPaginate: Boolean = canPaginate
        internal set
    var paginationCursor: String? = paginationCursor
        internal set

}