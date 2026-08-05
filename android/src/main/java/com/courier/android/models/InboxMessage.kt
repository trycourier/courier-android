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

    /**
     * Tracking id for a click on this message.
     *
     * Reads the root-level [trackingIds] only. Both the GraphQL read and the `iwpv=v2` socket
     * publish it there, so the two shapes finally agree.
     *
     * The nested `data["trackingIds"]` branch that used to be checked first is gone — this is the
     * "Riley's changes" the old TODO here was waiting on. It existed because the socket, on the
     * `legacy` protocol, nested tracking ids under `data` while GraphQL returned them at the root,
     * so this property had to look in both places and guess. Removing it is only safe because the
     * socket now speaks v2; on `legacy` the nested copy was the *only* one present.
     */
    val clickTrackingId: String? get() = trackingIds?.clickTrackingId

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

    /** Returns a timestamp in milliseconds */
    val timestamp: Long get() = createdAt.time

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