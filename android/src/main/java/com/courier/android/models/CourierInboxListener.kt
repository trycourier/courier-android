package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.modules.removeInboxListener
import com.courier.android.ui.inbox.InboxMessageFeed

class CourierInboxListener(
    val onLoading: ((isRefresh: Boolean) -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null,
    val onUnreadCountChanged: ((Int) -> Unit)? = null,
    val onFeedChanged: ((InboxMessageSet) -> Unit)? = null,
    val onArchiveChanged: ((InboxMessageSet) -> Unit)? = null,
    val onPageAdded: ((InboxMessageFeed, InboxMessageSet) -> Unit)? = null,
    val onMessageChanged: ((InboxMessageFeed, Int, InboxMessage) -> Unit)? = null,
    val onMessageAdded: ((InboxMessageFeed, Int, InboxMessage) -> Unit)? = null,
    val onMessageRemoved: ((InboxMessageFeed, Int, InboxMessage) -> Unit)? = null
) {

    private var isInitialized = false

    internal fun onLoad(data: CourierInboxData) {
        if (!isInitialized) return
        onFeedChanged?.invoke(data.feed)
        onArchiveChanged?.invoke(data.archived)
        onUnreadCountChanged?.invoke(data.unreadCount)
    }

    internal fun initialize() {
        onLoading?.invoke(false)
        isInitialized = true
    }

    fun remove() {
        Courier.shared.removeInboxListener(this)
    }

}
