package com.courier.android.models

data class CourierPushNotificationEvent(
    val trackingEvent: CourierTrackingEvent,
    val data: Map<String, String>
)