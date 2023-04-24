package com.courier.android.models

internal data class CourierInboxResponse(
    val data: InboxData?
)

internal data class InboxData(
    val count: Int? = 0,
    val messages: InboxNodes?
)

internal data class InboxNodes(
    val pageInfo: InboxPageInfo?,
    val nodes: List<InboxMessage>?
)

internal data class InboxPageInfo(
    val startCursor: String?,
    val hasNextPage: Boolean?
)