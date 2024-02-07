package com.courier.android.models

data class CourierTrackingIds(
    val archiveTrackingId: String?,
    val openTrackingId: String?,
    val clickTrackingId: String?,
    val deliverTrackingId: String?,
    val unreadTrackingId: String?,
    val readTrackingId: String?
)