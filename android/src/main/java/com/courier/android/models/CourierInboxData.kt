package com.courier.android.models

import com.courier.android.ui.inbox.InboxMessageFeed

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

    @Synchronized
    fun updateUnreadCount(count: Int) {
        this.unreadCount = count
    }

    @Synchronized
    fun addPage(feed: InboxMessageFeed, messageSet: InboxMessageSet) {
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
    fun addNewMessage(feed: InboxMessageFeed, index: Int, message: InboxMessage) {
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
    }
//
//
//    @Synchronized
//    fun readAllMessages(): ReadAllOperation {
//
//        // Copy previous values
//        val originalMessages = this.messages?.map { it.copy() }?.toMutableList()
//        val originalUnreadCount = this.unreadCount
//
//        // Read all messages
//        this.messages?.forEach { it.setRead() }
//        this.unreadCount = 0
//
//        return ReadAllOperation(
//            messages = originalMessages,
//            unreadCount = originalUnreadCount
//        )
//
//    }
//
//    // Return the index up the updated message
//    @Synchronized
//    fun readMessage(messageId: String): UpdateOperation {
//
//        if (messages == null) {
//            throw CourierException.inboxNotInitialized
//        }
//
//        val index = messages!!.indexOfFirst { it.messageId == messageId }
//
//        if (index == -1) {
//            throw CourierException.inboxMessageNotFound
//        }
//
//        // Save copy
//        val message = messages!![index]
//        val originalMessage = message.copy()
//        val originalUnreadCount = this.unreadCount
//
//        // Update
//        message.setRead()
//
//        // Change data
//        this.messages?.set(index, message)
//        this.unreadCount -= 1
//        this.unreadCount = this.unreadCount.coerceAtLeast(0)
//
//        return UpdateOperation(
//            index = index,
//            unreadCount = originalUnreadCount,
//            message = originalMessage
//        )
//
//    }
//
//    @Synchronized
//    fun unreadMessage(messageId: String): UpdateOperation {
//
//        if (messages == null) {
//            throw CourierException.inboxNotInitialized
//        }
//
//        val index = messages!!.indexOfFirst { it.messageId == messageId }
//
//        if (index == -1) {
//            throw CourierException.inboxMessageNotFound
//        }
//
//        // Save copy
//        val message = messages!![index]
//        val originalMessage = message.copy()
//        val originalUnreadCount = this.unreadCount
//
//        // Update
//        message.setUnread()
//
//        // Change data
//        this.messages?.set(index, message)
//        this.unreadCount += 1
//        this.unreadCount = this.unreadCount.coerceAtLeast(0)
//
//        return UpdateOperation(
//            index = index,
//            unreadCount = originalUnreadCount,
//            message = originalMessage
//        )
//
//    }
//
//    @Synchronized
//    fun resetReadAll(update: ReadAllOperation) {
//        this.messages = update.messages
//        this.unreadCount = update.unreadCount
//    }
//
//    @Synchronized
//    fun resetUpdate(update: UpdateOperation) {
//        this.messages?.set(update.index, update.message)
//        this.unreadCount = update.unreadCount
//    }

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

internal data class ReadAllOperation(
    val messages: MutableList<InboxMessage>?,
    val unreadCount: Int,
)

internal data class UpdateOperation(
    val index: Int,
    val unreadCount: Int,
    val message: InboxMessage,
)