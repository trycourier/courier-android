package com.courier.android.models

data class CourierInboxResponse(
    val data: InboxData?
)

data class InboxData(
    val count: Int? = 0,
    val messages: InboxNodes?
)

data class InboxNodes(
    val pageInfo: InboxPageInfo?,
    val nodes: List<InboxMessage>?
)

data class InboxPageInfo(
    val startCursor: String?,
    val hasNextPage: Boolean?
)