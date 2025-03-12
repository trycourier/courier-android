package com.courier.android.modules

import com.courier.android.client.CourierClient
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.ui.inbox.InboxMessageEvent
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.android.utils.log

internal class InboxDataStore {

    var delegate: InboxDataStoreEventDelegate? = null

    var feed: InboxMessageSet = InboxMessageSet()
        private set

    var archive: InboxMessageSet = InboxMessageSet()
        private set

    var unreadCount: Int = 0
        private set

    /** Creates an identical copy of the data */
    fun getSnapshot(): Triple<InboxMessageSet, InboxMessageSet, Int> {
        return Triple(feed, archive, unreadCount)
    }

    /** Reloads the data store from a snapshot */
    suspend fun reloadSnapshot(snapshot: Triple<InboxMessageSet, InboxMessageSet, Int>) {
        updateUnreadCount(snapshot.third)
        updateDataSet(snapshot.first, InboxMessageFeed.FEED)
        updateDataSet(snapshot.second, InboxMessageFeed.ARCHIVE)
    }

    /** Returns a message index by ID */
    fun getMessageIndexById(feedType: InboxMessageFeed, messageId: String): Int? {
        return when (feedType) {
            InboxMessageFeed.FEED -> feed.messages.indexOfFirst { it.messageId == messageId }.takeIf { it != -1 }
            InboxMessageFeed.ARCHIVE -> archive.messages.indexOfFirst { it.messageId == messageId }.takeIf { it != -1 }
        }
    }

    fun getMessageById(feedType: InboxMessageFeed, messageId: String): InboxMessage? {
        val index = getMessageIndexById(feedType, messageId) ?: return null
        return when (feedType) {
            InboxMessageFeed.FEED -> feed.messages[index]
            InboxMessageFeed.ARCHIVE -> archive.messages[index]
        }
    }

    /** Adds a message to either `feed` or `archive` */
    suspend fun addMessage(message: InboxMessage, index: Int, feedType: InboxMessageFeed) {
        when (feedType) {
            InboxMessageFeed.FEED -> {
                if (index in 0..feed.messages.size) {
                    feed.messages.add(index, message)
                } else {
                    feed.messages.add(message)
                }
                delegate?.onMessageEvent(message, index, feedType, InboxMessageEvent.ADDED)

                feed.totalCount += 1
                delegate?.onTotalCountUpdated(feed.totalCount, feedType)

                if (!message.isRead) {
                    unreadCount += 1
                    delegate?.onUnreadCountUpdated(unreadCount)
                }

                delegate?.onMessagesChanged(feed.messages, feed.canPaginate, feedType)
            }

            InboxMessageFeed.ARCHIVE -> {
                if (index in 0..archive.messages.size) {
                    archive.messages.add(index, message)
                } else {
                    archive.messages.add(message)
                }
                delegate?.onMessageEvent(message, index, feedType, InboxMessageEvent.ADDED)

                archive.totalCount += 1
                delegate?.onTotalCountUpdated(archive.totalCount, feedType)

                delegate?.onMessagesChanged(archive.messages, archive.canPaginate, feedType)
            }
        }
    }

    /** Reads a message */
    suspend fun readMessage(message: InboxMessage, feedType: InboxMessageFeed, client: CourierClient?): Boolean {
        val snapshot = getSnapshot()
        var canUpdate = false

        val index = getMessageIndexById(feedType, message.messageId) ?: return false
        val msg = when (feedType) {
            InboxMessageFeed.FEED -> feed.messages[index]
            InboxMessageFeed.ARCHIVE -> archive.messages[index]
        }

        if (!msg.isRead) {
            msg.setRead()
            delegate?.onMessageEvent(msg, index, feedType, InboxMessageEvent.READ)

            if (feedType == InboxMessageFeed.FEED) {
                unreadCount -= 1
                delegate?.onUnreadCountUpdated(unreadCount)
            }

            delegate?.onMessagesChanged(
                if (feedType == InboxMessageFeed.FEED) feed.messages else archive.messages,
                feedType == InboxMessageFeed.FEED && feed.canPaginate,
                feedType
            )

            canUpdate = true
        }

        if (canUpdate) {
            try {
                client?.inbox?.read(message.messageId)
            } catch (e: Exception) {
                client?.log(e.localizedMessage ?: "Error")
                reloadSnapshot(snapshot)
            }
        }
        return canUpdate
    }

    suspend fun unreadMessage(message: InboxMessage, feedType: InboxMessageFeed, client: CourierClient?): Boolean {
        val snapshot = getSnapshot()
        var canUpdate = false

        val index = getMessageIndexById(feedType, message.messageId) ?: return false

        when (feedType) {
            InboxMessageFeed.FEED -> {
                val msg = feed.messages[index]
                if (msg.isRead) {
                    msg.setUnread()
                    delegate?.onMessageEvent(msg, index, feedType, InboxMessageEvent.UNREAD)

                    // Update unread count
                    unreadCount += 1
                    delegate?.onUnreadCountUpdated(unreadCount)
                    delegate?.onMessagesChanged(feed.messages, feed.canPaginate, feedType)

                    canUpdate = true
                }
            }

            InboxMessageFeed.ARCHIVE -> {
                val msg = archive.messages[index]
                if (msg.isRead) {
                    msg.setUnread()
                    delegate?.onMessageEvent(msg, index, feedType, InboxMessageEvent.UNREAD)
                    delegate?.onMessagesChanged(archive.messages, archive.canPaginate, feedType)

                    canUpdate = true
                }
            }
        }

        if (canUpdate) {
            try {
                client?.inbox?.unread(message.messageId)
            } catch (e: Exception) {
                client?.log(e.localizedMessage ?: "Error marking message as unread")
                reloadSnapshot(snapshot)
            }
        }

        return canUpdate
    }

    suspend fun openMessage(message: InboxMessage, feedType: InboxMessageFeed, client: CourierClient?): Boolean {
        val snapshot = getSnapshot()
        var canUpdate = false

        val index = getMessageIndexById(feedType, message.messageId) ?: return false

        when (feedType) {
            InboxMessageFeed.FEED -> {
                val msg = feed.messages[index]
                if (!msg.isOpened) {
                    msg.setOpened()
                    delegate?.onMessageEvent(msg, index, feedType, InboxMessageEvent.OPENED)
                    delegate?.onMessagesChanged(feed.messages, feed.canPaginate, feedType)
                    canUpdate = true
                }
            }

            InboxMessageFeed.ARCHIVE -> {
                val msg = archive.messages[index]
                if (!msg.isOpened) {
                    msg.setOpened()
                    delegate?.onMessageEvent(msg, index, feedType, InboxMessageEvent.OPENED)
                    delegate?.onMessagesChanged(archive.messages, archive.canPaginate, feedType)
                    canUpdate = true
                }
            }
        }

        if (canUpdate) {
            try {
                client?.inbox?.open(message.messageId)
            } catch (e: Exception) {
                client?.log(e.localizedMessage ?: "Error opening message")
                reloadSnapshot(snapshot)
            }
        }

        return canUpdate
    }

    suspend fun archiveMessage(message: InboxMessage, feedType: InboxMessageFeed, client: CourierClient?): Boolean {
        val snapshot = getSnapshot()
        var canUpdate = false

        val index = getMessageIndexById(feedType, message.messageId) ?: return false

        when (feedType) {
            InboxMessageFeed.FEED -> {
                // Update unread count if the message is unread
                if (!feed.messages[index].isRead) {
                    unreadCount -= 1
                    delegate?.onUnreadCountUpdated(unreadCount)
                }

                val msg = feed.messages[index]

                // Remove message from feed
                feed.messages.removeAt(index)
                delegate?.onMessageEvent(msg, index, feedType, InboxMessageEvent.ARCHIVED)

                // Update feed total counts
                feed.totalCount -= 1
                delegate?.onTotalCountUpdated(feed.totalCount, InboxMessageFeed.FEED)
                delegate?.onMessagesChanged(feed.messages, feed.canPaginate, InboxMessageFeed.FEED)

                // Create copy and mark as archived
                msg.setArchived()
                val newMessage = msg.copy()

                // Add the item to the archive
                val insertIndex = findInsertIndex(newMessage, archive.messages)
                archive.messages.add(insertIndex, newMessage)
                delegate?.onMessageEvent(newMessage, insertIndex, InboxMessageFeed.ARCHIVE, InboxMessageEvent.ADDED)

                // Update archive total counts
                archive.totalCount += 1
                delegate?.onTotalCountUpdated(archive.totalCount, InboxMessageFeed.ARCHIVE)
                delegate?.onMessagesChanged(archive.messages, archive.canPaginate, InboxMessageFeed.ARCHIVE)

                canUpdate = true
            }

            InboxMessageFeed.ARCHIVE -> return false
        }

        if (canUpdate) {
            try {
                client?.inbox?.archive(message.messageId)
            } catch (e: Exception) {
                client?.log(e.localizedMessage ?: "Error archiving message")
                reloadSnapshot(snapshot)
            }
        }

        return canUpdate
    }

    suspend fun clickMessage(message: InboxMessage, feedType: InboxMessageFeed, client: CourierClient?): Boolean {
        val index = getMessageIndexById(feedType, message.messageId) ?: return false

        val trackingId = when (feedType) {
            InboxMessageFeed.FEED -> feed.messages[index].clickTrackingId
            InboxMessageFeed.ARCHIVE -> archive.messages[index].clickTrackingId
        }

        // Perform server update
        return if (trackingId != null) {
            try {
                client?.inbox?.click(message.messageId, trackingId)
                true
            } catch (e: Exception) {
                client?.log(e.localizedMessage ?: "Error clicking message")
                false
            }
        } else {
            false
        }
    }

    suspend fun readAllMessages(client: CourierClient?): Boolean {
        val snapshot = getSnapshot()

        // Read all messages in the feed
        feed.messages.forEachIndexed { index, message ->
            if (!message.isRead) {
                message.setRead()
                delegate?.onMessageEvent(message, index, InboxMessageFeed.FEED, InboxMessageEvent.READ)
            }
        }

        // Read all messages in the archive
        archive.messages.forEachIndexed { index, message ->
            if (!message.isRead) {
                message.setRead()
                delegate?.onMessageEvent(message, index, InboxMessageFeed.ARCHIVE, InboxMessageEvent.READ)
            }
        }

        // Update unread count
        unreadCount = 0
        delegate?.onUnreadCountUpdated(unreadCount)
        delegate?.onMessagesChanged(feed.messages, feed.canPaginate, InboxMessageFeed.FEED)
        delegate?.onMessagesChanged(archive.messages, archive.canPaginate, InboxMessageFeed.ARCHIVE)

        return try {
            client?.inbox?.readAll()
            true
        } catch (e: Exception) {
            client?.log(e.localizedMessage ?: "Error reading all messages")
            reloadSnapshot(snapshot)
            false
        }
    }

    suspend fun addPage(page: InboxMessageSet, feedType: InboxMessageFeed) {
        when (feedType) {
            InboxMessageFeed.FEED -> {
                feed.totalCount = page.totalCount
                feed.canPaginate = page.canPaginate
                feed.paginationCursor = page.paginationCursor
                feed.messages.addAll(page.messages)

                delegate?.onPageAdded(page.messages, page.canPaginate, isFirstPage = false, feedType)
                delegate?.onTotalCountUpdated(feed.totalCount, feedType)
                delegate?.onMessagesChanged(feed.messages, feed.canPaginate, feedType)
            }

            InboxMessageFeed.ARCHIVE -> {
                archive.totalCount = page.totalCount
                archive.canPaginate = page.canPaginate
                archive.paginationCursor = page.paginationCursor
                archive.messages.addAll(page.messages)

                delegate?.onPageAdded(page.messages, page.canPaginate, isFirstPage = false, feedType)
                delegate?.onTotalCountUpdated(archive.totalCount, feedType)
                delegate?.onMessagesChanged(archive.messages, archive.canPaginate, feedType)
            }
        }
    }

    /** Updates unread count */
    suspend fun updateUnreadCount(count: Int) {
        unreadCount = count
        delegate?.onUnreadCountUpdated(count)
    }

    /** Removes and resets everything */
    suspend fun dispose() {
        updateDataSet(InboxMessageSet(), InboxMessageFeed.FEED)
        updateDataSet(InboxMessageSet(), InboxMessageFeed.ARCHIVE)
        updateUnreadCount(0)
    }

    private fun findInsertIndex(newMessage: InboxMessage, messages: List<InboxMessage>): Int {
        // Create a mutable copy of the messages list
        val allMessages = messages.toMutableList()

        // Add the new message to the list
        allMessages.add(newMessage)

        // Sort messages by `timestamp` in descending order
        allMessages.sortByDescending { it.timestamp }

        // Find the index of the newly inserted message
        val index = allMessages.indexOfFirst { it.messageId == newMessage.messageId }

        // Ensure the index is not out of bounds
        return if (index != -1) maxOf(index - 1, 0) else 0
    }

    /** Updates the dataset */
    suspend fun updateDataSet(data: InboxMessageSet, feedType: InboxMessageFeed) {
        when (feedType) {
            InboxMessageFeed.FEED -> {
                feed = data
                delegate?.onTotalCountUpdated(feed.totalCount, feedType)
                delegate?.onPageAdded(data.messages, data.canPaginate, true, feedType)
                delegate?.onMessagesChanged(feed.messages, feed.canPaginate, feedType)
            }

            InboxMessageFeed.ARCHIVE -> {
                archive = data
                delegate?.onTotalCountUpdated(archive.totalCount, feedType)
                delegate?.onPageAdded(data.messages, data.canPaginate, true, feedType)
                delegate?.onMessagesChanged(archive.messages, archive.canPaginate, feedType)
            }
        }
    }
}