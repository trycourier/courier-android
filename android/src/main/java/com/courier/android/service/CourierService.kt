package com.courier.android.service

import com.courier.android.Courier
import com.courier.android.utils.broadcastMessage
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFCMToken
import com.courier.android.utils.trackNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

open class CourierService: FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()

        // Init the SDK if needed
        Courier.initialize(context = this)

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        try {

            // Track the event in Courier
            // The payload being sent to the device must contain only data
            // If the payload contains title and body, there will be
            // issues tracking the event
            // More info: https://stackoverflow.com/a/71253912/2415921
            Courier.shared.trackNotification(
                message = message,
                event = CourierTrackingEvent.DELIVERED,
                onSuccess = { Courier.log("Event tracked") },
                onFailure = { Courier.error(it.toString()) }
            )

            // Broadcast the message to the app
            // This will allow us to handle when it's delivered
            Courier.shared.broadcastMessage(message)

        } catch (e: Exception) {

            Courier.error(e.toString())

        }

        // Try and show the notification
        showNotification(message)

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        try {

            Courier.shared.setFCMToken(
                token = token,
                onSuccess = { Courier.log("Courier FCM token updated") },
                onFailure = { Courier.error(it.toString()) }
            )

        } catch (e: Exception) {

            Courier.error(e.toString())

        }

    }

    open fun showNotification(message: RemoteMessage) {
        // Empty
    }

}