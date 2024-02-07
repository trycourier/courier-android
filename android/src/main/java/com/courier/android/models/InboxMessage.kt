package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.modules.readMessage
import com.courier.android.modules.unreadMessage
import com.courier.android.utils.isoToDate
import com.courier.android.utils.timeSince
import com.courier.android.utils.toIsoTimestamp
import java.util.*

data class InboxMessage(
    val messageId: String,
    val title: String?,
    val body: String?,
    val preview: String?,
    val created: String?,
    val actions: List<InboxAction>?,
    val data: Map<String, Any>?,
    val archived: Boolean?,
    var read: String?,
    var opened: String?,
    private val trackingIds: CourierTrackingIds?
) {

    val subtitle get() = body ?: preview
    val isRead get() = read != null
    val isOpened get() = opened != null
    val isArchived get() = archived != null

    private val trackingIdsData: Map<String, Any>? get() = data?.get("trackingIds") as? Map<String, Any>?

    val archiveTrackingId: String? get() = trackingIdsData?.get("archiveTrackingId") as? String ?: trackingIds?.archiveTrackingId
    val openTrackingId: String? get() = trackingIdsData?.get("openTrackingId") as? String ?: trackingIds?.openTrackingId
    val clickTrackingId: String? get() = trackingIdsData?.get("clickTrackingId") as? String ?: trackingIds?.clickTrackingId
    val deliverTrackingId: String? get() = trackingIdsData?.get("deliverTrackingId") as? String ?: trackingIds?.deliverTrackingId
    val unreadTrackingId: String? get() = trackingIdsData?.get("unreadTrackingId") as? String ?: trackingIds?.unreadTrackingId
    val readTrackingId: String? get() = trackingIdsData?.get("readTrackingId") as? String ?: trackingIds?.readTrackingId

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
    Courier.shared.readMessage(messageId = messageId, onFailure = null)
}

fun InboxMessage.markAsUnread() {
    Courier.shared.unreadMessage(messageId = messageId, onFailure = null)
}