package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.isoToDate
import com.courier.android.timeSince
import com.courier.android.toIsoTimestamp
import java.util.*

data class InboxMessage(
    val messageId: String,
    val title: String?,
    val body: String?,
    val preview: String?,
    val created: String?,
    val actions: List<InboxAction>?,
    val data: Map<String, Any>?,
    internal val archived: Boolean?,
    internal var read: String?,
    internal var opened: String?,
) {

    val subtitle get() = body ?: preview
    val isRead get() = read != null
    val isOpened get() = opened != null
    val isArchived get() = archived != null

    internal fun setRead() {
        read = Date().toIsoTimestamp()
    }

    internal fun setUnread() {
        read = null
    }

    internal fun setOpened() {
        opened = Date().toIsoTimestamp()
    }

    val time: String get() {

        val date = created?.isoToDate()

        if (created == null || date == null) {
            return "now"
        }

        return date.timeSince()

    }

}

/**
 * Extensions
 */

fun InboxMessage.markAsRead() {
    Courier.shared.inbox.readMessage(messageId)
}

fun InboxMessage.markAsUnread() {
    Courier.shared.inbox.unreadMessage(messageId)
}