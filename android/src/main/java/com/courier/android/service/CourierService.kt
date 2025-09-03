package com.courier.android.service

import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.PushNotification
import com.courier.android.modules.setFcmToken
import com.courier.android.utils.trackAndBroadcastTheEvent
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.courier.android.utils.toPushNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

open class CourierService: FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()

        // Init the SDK if needed
        Courier.initialize(context = this)

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {

            val pushNotification = remoteMessage.toPushNotification()

            // Broadcast the message to the app
            // This will allow us to handle when it's delivered
            Courier.shared.trackAndBroadcastTheEvent(
                trackingEvent = CourierTrackingEvent.DELIVERED,
                pushNotification = pushNotification
            )

        } catch (e: Exception) {

            CourierClient.default.error(e.toString())

        }

        // Try and show the notification
        val pushNotification = remoteMessage.toPushNotification()
        showNotification(pushNotification)

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        try {

            Courier.shared.setFcmToken(
                token = token,
                onSuccess = { Courier.shared.client?.log("Courier FCM token updated") },
                onFailure = { Courier.shared.client?.error(it.toString()) }
            )

        } catch (e: Exception) {

            Courier.shared.client?.error(e.toString())

        }

    }

    open fun showNotification(pushNotification: PushNotification) {
        // Empty
    }

}