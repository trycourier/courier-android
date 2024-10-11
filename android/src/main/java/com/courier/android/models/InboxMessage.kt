package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.modules.archiveMessage
import com.courier.android.modules.clickMessage
import com.courier.android.modules.openMessage
import com.courier.android.modules.readMessage
import com.courier.android.modules.unreadMessage
import com.courier.android.utils.isoToDate
import com.courier.android.utils.timeSince
import com.courier.android.utils.toIsoTimestamp
import java.util.Date

data class InboxMessage(
    val messageId: String,
    val title: String? = null,
    val body: String? = null,
    val preview: String? = null,
    val actions: List<InboxAction>? = null,
    val data: Map<String, Any>? = null,
    val created: String? = null,
    var archived: String? = null,
    var read: String? = null,
    var opened: String? = null,
    val trackingIds: CourierTrackingIds? = null,
) {

    val subtitle get() = body ?: preview
    val isRead get() = read != null
    val isOpened get() = opened != null
    val isArchived get() = archived != null

    // TODO: Make this cleaner. Need Riley's Changes.
    private val trackingIdsData: Map<String, Any>? get() = data?.get("trackingIds") as? Map<String, Any>?
    val clickTrackingId: String? get() = trackingIdsData?.get("clickTrackingId") as? String ?: trackingIds?.clickTrackingId

    internal fun setRead() {
        read = Date().toIsoTimestamp()
    }

    internal fun setUnread() {
        read = null
    }

    internal fun setOpened() {
        opened = Date().toIsoTimestamp()
    }

    internal fun setUnopened() {
        opened = null
    }

    internal fun setArchived() {
        archived = Date().toIsoTimestamp()
    }

    internal fun setUnarchived() {
        archived = null
    }

    val createdAt: Date get() = created?.isoToDate() ?: Date()

    val time: String get() = createdAt.timeSince()

}

/**
 * Extensions
 */

fun InboxMessage.markAsRead() {
    Courier.shared.readMessage(messageId = messageId, onFailure = null)
}

fun InboxMessage.markAsUnread() {
    Courier.shared.unreadMessage(messageId = messageId, onFailure = null)
}

fun InboxMessage.markAsOpened() {
    Courier.shared.openMessage(messageId = messageId, onFailure = null)
}

fun InboxMessage.markAsClicked() {
    Courier.shared.clickMessage(messageId = messageId, onFailure = null)
}

fun InboxMessage.markAsArchived() {
    Courier.shared.archiveMessage(messageId = messageId, onFailure = null)
}