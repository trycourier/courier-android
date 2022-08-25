package com.courier.android.service

import com.courier.android.Courier
import com.courier.android.broadcastMessage
import com.courier.android.log
import com.courier.android.models.CourierPushEvent
import com.courier.android.trackNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

open class CourierService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Track the event in Courier
        // The payload being sent to the device must contain only data
        // If the payload contains title and body, there will be
        // issues tracking the event
        // More info: https://stackoverflow.com/a/71253912/2415921
        Courier.instance.trackNotification(
            message = message,
            event = CourierPushEvent.DELIVERED,
            onSuccess = { Courier.log("Event tracked") },
            onFailure = { Courier.log(it.toString()) }
        )

        // Broadcast the message to the app
        // This will allow us to handle when it's delivered
        Courier.instance.broadcastMessage(message)

        // Try and show the notification
        showNotification(message)

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Courier.instance.setFCMToken(
            token = token,
            onSuccess = { Courier.log("Courier FCM token refreshed") },
            onFailure = { Courier.log(it.toString()) }
        )
    }

    open fun showNotification(message: RemoteMessage) {
        // Empty
    }

}