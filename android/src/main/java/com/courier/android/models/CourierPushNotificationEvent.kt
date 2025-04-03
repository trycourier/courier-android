package com.courier.android.models

import com.google.firebase.messaging.RemoteMessage

data class CourierPushNotificationEvent(
    val trackingEvent: CourierTrackingEvent,
    val remoteMessage: RemoteMessage
)