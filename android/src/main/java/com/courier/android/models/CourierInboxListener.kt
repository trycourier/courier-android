package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.modules.removeInboxListener
import com.courier.android.ui.inbox.InboxMessageEvent
import com.courier.android.ui.inbox.InboxMessageFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourierInboxListener(
    val onLoading: ((isRefresh: Boolean) -> Unit)? = null,
    val onError: ((error: Throwable) -> Unit)? = null,
    val onUnreadCountChanged: ((unreadCount: Int) -> Unit)? = null,
    val onTotalCountChanged: ((totalCount: Int, feed: InboxMessageFeed) -> Unit)? = null,
    val onMessagesChanged: ((messages: List<InboxMessage>, canPaginate: Boolean, feed: InboxMessageFeed) -> Unit)? = null,
    val onPageAdded: ((messages: List<InboxMessage>, canPaginate: Boolean, isFirstPage: Boolean, feed: InboxMessageFeed) -> Unit)? = null,
    val onMessageEvent: ((message: InboxMessage, index: Int, feed: InboxMessageFeed, event: InboxMessageEvent) -> Unit)? = null,
) {

    private var isInitialized = false

    internal fun onLoad(snapshot: Triple<InboxMessageSet, InboxMessageSet, Int>) {
        if (!isInitialized) return

        val (feed, archive, unreadCount) = snapshot

        onPageAdded?.invoke(feed.messages, feed.canPaginate, true, InboxMessageFeed.FEED)
        onPageAdded?.invoke(archive.messages, archive.canPaginate, true, InboxMessageFeed.ARCHIVE)
        onMessagesChanged?.invoke(feed.messages, feed.canPaginate, InboxMessageFeed.FEED)
        onMessagesChanged?.invoke(archive.messages, archive.canPaginate, InboxMessageFeed.ARCHIVE)
        onTotalCountChanged?.invoke(feed.totalCount, InboxMessageFeed.FEED)
        onTotalCountChanged?.invoke(archive.totalCount, InboxMessageFeed.ARCHIVE)
        onUnreadCountChanged?.invoke(unreadCount)
    }

    internal fun initialize() {
        onLoading?.invoke(false)
        isInitialized = true
    }

    fun remove() {
        Courier.coroutineScope.launch(Dispatchers.IO) {
            Courier.shared.removeInboxListener(this@CourierInboxListener)
        }
    }

}
