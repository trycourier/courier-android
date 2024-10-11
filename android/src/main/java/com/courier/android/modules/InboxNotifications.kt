package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.models.CourierInboxData
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.android.utils.toCourierException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun Courier.runOnMain(block: suspend () -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    block()
}

internal fun Courier.notifyLoading(isRefresh: Boolean) = runOnMain {
    inboxListeners.forEach {
        it.onLoading?.invoke(isRefresh)
    }
}

internal fun Courier.notifyError(error: Exception) = runOnMain {
    inboxListeners.forEach {
        it.onError?.invoke(error.toCourierException)
    }
}

internal fun Courier.notifyInboxUpdated(inboxData: CourierInboxData) = runOnMain {
    inboxListeners.forEach {
        it.onFeedChanged?.invoke(inboxData.feed)
        it.onArchiveChanged?.invoke(inboxData.archived)
    }
}

internal fun Courier.notifyUnreadCountChange(count: Int) = runOnMain {
    inboxListeners.forEach {
        it.onUnreadCountChanged?.invoke(count)
    }
}

internal fun Courier.notifyPageAdded(feed: InboxMessageFeed, messageSet: InboxMessageSet) = runOnMain {
    inboxListeners.forEach {
        it.onPageAdded?.invoke(feed, messageSet)
    }
}

internal fun Courier.notifyMessageAdded(feed: InboxMessageFeed, index: Int, message: InboxMessage) = runOnMain {
    inboxListeners.forEach {
        it.onMessageAdded?.invoke(feed, index, message)
    }
}

internal fun Courier.notifyMessageUpdated(feed: InboxMessageFeed, index: Int, message: InboxMessage) = runOnMain {
    inboxListeners.forEach {
        it.onMessageChanged?.invoke(feed, index, message)
    }
}

internal fun Courier.notifyMessageRemoved(feed: InboxMessageFeed, index: Int, message: InboxMessage) = runOnMain {
    inboxListeners.forEach {
        it.onMessageRemoved?.invoke(feed, index, message)
    }
}