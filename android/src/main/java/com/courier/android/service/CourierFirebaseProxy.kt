package com.courier.android.service

import android.content.Intent
import android.os.Bundle
import com.courier.android.Courier
import com.courier.android.activity.CourierActivity
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFcmToken
import com.courier.android.notifications.present
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.courier.android.utils.toPushNotification
import com.courier.android.utils.trackAndBroadcastTheEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

object CourierActions {
    const val MESSAGE_RECEIVED = "com.trycourier.MESSAGE_RECEIVED"
    const val TOKEN_UPDATED = "com.trycourier.TOKEN_UPDATED"
}

// Your auto-generated Firebase service (created at runtime)
class CourierFirebaseProxy : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {

            // Broadcast to ALL manifest-declared receivers listening for this action
            broadcastToManifestReceivers(message)

            // This will allow us to handle when it's delivered
            Courier.shared.trackAndBroadcastTheEvent(
                trackingEvent = CourierTrackingEvent.DELIVERED,
                message = message
            )

        } catch (e: Exception) {
            CourierClient.default.error(e.toString())
        }
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
            CourierClient.default.error(e.toString())
        }
    }

    private fun broadcastToManifestReceivers(message: RemoteMessage) {
        val intent = Intent(CourierActions.MESSAGE_RECEIVED).apply {

            // Add message data
            putExtra("title", message.data["title"] ?: message.notification?.title)
            putExtra("body", message.data["body"] ?: message.notification?.body)
            putExtra("from", message.from)

            val dataBundle = Bundle().apply {
                message.data.forEach { (key, value) -> putString(key, value) }
            }
            putExtra("data", dataBundle)

            // CRITICAL: This flag makes it work in killed state
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }

        // Send explicit broadcasts to all receivers that declared this action
        sendBroadcastToManifestReceivers(intent)
    }

    private fun sendBroadcastToManifestReceivers(intent: Intent) {
        // Get all receivers that can handle this intent from the manifest
        val packageManager = packageManager
        val receivers = packageManager.queryBroadcastReceivers(intent, 0)

        // Send explicit intent to each receiver (works better for killed apps)
        receivers.forEach { resolveInfo ->
            val explicitIntent = Intent(intent).apply {
                setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
            }
            sendBroadcast(explicitIntent)
        }

        // Also send the implicit broadcast as backup
        sendBroadcast(intent)
    }
}