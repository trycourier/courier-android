package com.courier.android.service

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
        // The payload being sent to the device must contain only data
        // If the payload contains title and body, there will be
        // issues tracking the event
        // More info: https://stackoverflow.com/a/71253912/2415921
        Courier.trackNotification(
            message = message,
            event = CourierPushEvent.DELIVERED,
            onSuccess = { Courier.log("Event tracked") },
            onFailure = { Courier.log(it.toString()) }
        )

        showNotification(message)

    }

    override fun onNewToken(token: String) {
        Courier.instance.setFCMToken(
            token = token,
            onSuccess = { Courier.log("Courier FCM token refreshed") },
            onFailure = { Courier.log(it.toString()) }
        )
        super.onNewToken(token)
    }

    open fun showNotification(message: RemoteMessage) {
        // Empty
    }

}