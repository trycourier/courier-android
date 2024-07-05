package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.utils.trackClick

data class InboxAction(
    val content: String?,
    val href: String?,
    val data: Map<String, Any>?,
)

/**
 * Extensions
 */

fun InboxAction.markAsClicked(messageId: String) {
    (data?.get("trackingId") as? String)?.let { trackingId ->
        Courier.shared.trackClick(messageId = messageId, trackingId = trackingId, onFailure = null)
    }
}