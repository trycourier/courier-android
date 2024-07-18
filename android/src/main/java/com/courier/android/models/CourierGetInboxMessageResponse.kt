package com.courier.android.models

data class CourierGetInboxMessageResponse(
    val data: GetInboxMessageData?
)

data class GetInboxMessageData(
    val message: InboxMessage
)