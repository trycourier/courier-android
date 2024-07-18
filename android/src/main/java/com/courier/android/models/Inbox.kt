package com.courier.android.models

internal data class Inbox(
    var messages: MutableList<InboxMessage>?,
    var totalCount: Int,
    var unreadCount: Int,
    var hasNextPage: Boolean?,
    var startCursor: String?,
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

        if (messages == null) {
            throw CourierException.inboxNotInitialized
        }

        val index = messages!!.indexOfFirst { it.messageId == messageId }

        if (index == -1) {
            throw CourierException.inboxMessageNotFound
        }

        // Save copy
        val message = messages!![index]
        val originalMessage = message.copy()
        val originalUnreadCount = this.unreadCount

        // Update
        message.setRead()

        // Change data
        this.messages?.set(index, message)
        this.unreadCount -= 1
        this.unreadCount = this.unreadCount.coerceAtLeast(0)

        return UpdateOperation(
            index = index,
            unreadCount = originalUnreadCount,
            message = originalMessage
        )

    }

    fun unreadMessage(messageId: String): UpdateOperation {

        if (messages == null) {
            throw CourierException.inboxNotInitialized
        }

        val index = messages!!.indexOfFirst { it.messageId == messageId }

        if (index == -1) {
            throw CourierException.inboxMessageNotFound
        }

        // Save copy
        val message = messages!![index]
        val originalMessage = message.copy()
        val originalUnreadCount = this.unreadCount

        // Update
        message.setUnread()

        // Change data
        this.messages?.set(index, message)
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

internal data class ReadAllOperation(
    val messages: MutableList<InboxMessage>?,
    val unreadCount: Int,
)

internal data class UpdateOperation(
    val index: Int,
    val unreadCount: Int,
    val message: InboxMessage,
)