package com.courier.android.notifications

import com.google.firebase.messaging.RemoteMessage

interface CourierPushNotificationCallbacks {
    fun onPushNotificationClicked(message: RemoteMessage) {}
    fun onPushNotificationDelivered(message: RemoteMessage) {}
}