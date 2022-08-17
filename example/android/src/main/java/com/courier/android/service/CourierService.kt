package com.courier.android.service

import android.util.Log
import com.courier.android.Courier
import com.courier.android.log
import com.courier.android.models.CourierPushEvent
import com.courier.android.trackNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

open class CourierService: FirebaseMessagingService() {

    companion object {
        private const val TAG = "CourierService"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Track the event in Courier
        Courier.trackNotification(
            message = message,
            event = CourierPushEvent.DELIVERED,
            onSuccess = { Courier.log("Event tracked") },
            onFailure = { Courier.log(it.toString()) }
        )

        // Check if message contains a notification payload.
        message.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // TODO: MESSAGE RECEIVED CALLBACK

    }

    override fun onNewToken(token: String) {
        Courier.instance.setFCMToken(
            token = token,
            onSuccess = { Courier.log("Courier FCM token refreshed") },
            onFailure = { Courier.log(it.toString()) }
        )
    }

}