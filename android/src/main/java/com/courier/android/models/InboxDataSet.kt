package com.courier.android.models

data class InboxMessageSet(
    var messages: MutableList<InboxMessage> = mutableListOf(),
    var totalCount: Int = 0,
    var canPaginate: Boolean = false,
    var paginationCursor: String? = null
)