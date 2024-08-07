package com.courier.android.models

import com.courier.android.Courier
import kotlinx.coroutines.launch

data class InboxAction(
    val content: String?,
    val href: String?,
    val data: Map<String, Any>?,
)

/**
 * Extensions
 */

fun InboxAction.markAsClicked(messageId: String) = Courier.coroutineScope.launch {
    (data?.get("trackingId") as? String)?.let { trackingId ->
        Courier.shared.client?.inbox?.click(messageId = messageId, trackingId = trackingId)
    }
}