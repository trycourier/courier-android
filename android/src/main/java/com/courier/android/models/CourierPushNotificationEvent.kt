package com.courier.android.models

data class CourierPushNotificationEvent(
    val trackingEvent: CourierTrackingEvent,
    val pushNotification: PushNotification
)