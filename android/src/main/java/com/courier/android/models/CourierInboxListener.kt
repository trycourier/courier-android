package com.courier.android.models

import com.courier.android.Courier

data class CourierInboxListener(
    val onInitialLoad: (() -> Unit)?,
    val onError: ((CourierException) -> Unit)?,
    val onMessagesChanged: ((messages: List<InboxMessage>, unreadMessageCount: Int, totalMessageCount: Int, canPaginate: Boolean) -> Unit)?,
) {
    var isInitialized = false
}

/**
 * Extensions
 */

internal fun CourierInboxListener.initialize() {
    onInitialLoad?.invoke()
    isInitialized = true
}

fun CourierInboxListener.remove() {
    Courier.shared.inbox.removeInboxListener(listener = this)
}