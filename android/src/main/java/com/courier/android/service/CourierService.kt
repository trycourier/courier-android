package com.courier.android.service

import com.courier.android.Courier
import com.courier.android.client.error
import com.courier.android.client.log
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFCMToken
import com.courier.android.utils.broadcastMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            message.data["trackingUrl"]?.let { trackingUrl ->
                Courier.coroutineScope.launch(Dispatchers.IO) {
                    Courier.shared.client?.tracking?.postTrackingUrl(
                        url = trackingUrl,
                        event = CourierTrackingEvent.DELIVERED,
                    )
                }
            }

            // Broadcast the message to the app
            // This will allow us to handle when it's delivered
            Courier.shared.broadcastMessage(message)

        } catch (e: Exception) {

            Courier.shared.client?.options?.error(e.toString())

        }

        // Try and show the notification
        showNotification(message)

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        try {

            Courier.shared.setFCMToken(
                token = token,
                onSuccess = { Courier.shared.client?.options?.log("Courier FCM token updated") },
                onFailure = { Courier.shared.client?.options?.error(it.toString()) }
            )

        } catch (e: Exception) {

            Courier.shared.client?.options?.error(e.toString())

        }

    }

    open fun showNotification(message: RemoteMessage) {
        // Empty
    }

}