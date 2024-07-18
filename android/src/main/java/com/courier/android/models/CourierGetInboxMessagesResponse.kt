package com.courier.android.models

data class CourierGetInboxMessagesResponse(
    val data: GetInboxMessagesData?
)

data class GetInboxMessagesData(
    val count: Int? = 0,
    val messages: GetInboxMessagesNodes?
)

data class GetInboxMessagesNodes(
    val pageInfo: GetInboxMessagesPageInfo?,
    val nodes: List<InboxMessage>?
)

data class GetInboxMessagesPageInfo(
    val startCursor: String?,
    val hasNextPage: Boolean?
)