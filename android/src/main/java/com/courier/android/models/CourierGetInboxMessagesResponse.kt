package com.courier.android.models

data class CourierGetInboxMessagesResponse(
    val data: GetInboxMessagesData?
)

fun CourierGetInboxMessagesResponse.toMessageSet(): InboxMessageSet {
    return InboxMessageSet(
        messages = data?.messages?.nodes ?: emptyList(),
        totalCount = data?.count ?: 0,
        canPaginate = data?.messages?.pageInfo?.hasNextPage ?: false,
        paginationCursor = data?.messages?.pageInfo?.startCursor
    )
}

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