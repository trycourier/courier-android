package com.courier.android.models

data class InboxMessage(
    val messageId: String,
    val title: String?,
    val body: String?,
    val preview: String?,
    val created: String?,
    val actions: List<InboxAction>?,
    val data: Map<String, Any>?,
    internal val archived: Boolean?,
    internal val read: String?,
    internal val opened: String?,
) {
    val subtitle get() = body ?: preview
}