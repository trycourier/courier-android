package com.courier.android.modules

import com.courier.android.models.InboxMessage
import com.courier.android.ui.inbox.InboxMessageEvent
import com.courier.android.ui.inbox.InboxMessageFeed

internal interface InboxDataStoreEventDelegate {
    suspend fun onLoading(isRefresh: Boolean)
    suspend fun onError(error: Throwable)
    suspend fun onMessagesChanged(messages: List<InboxMessage>, canPaginate: Boolean, feed: InboxMessageFeed)
    suspend fun onPageAdded(messages: List<InboxMessage>, canPaginate: Boolean, isFirstPage: Boolean, feed: InboxMessageFeed)
    suspend fun onMessageEvent(message: InboxMessage, index: Int, feed: InboxMessageFeed, event: InboxMessageEvent)
    suspend fun onTotalCountUpdated(totalCount: Int, feed: InboxMessageFeed)
    suspend fun onUnreadCountUpdated(unreadCount: Int)
}