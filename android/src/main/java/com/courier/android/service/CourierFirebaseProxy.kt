package com.courier.android.service

import android.content.Intent
import android.os.Bundle
import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFcmToken
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.courier.android.utils.trackAndBroadcastTheEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class CourierFirebaseProxy : FirebaseMessagingService() {

    object Events {
        const val PUSH_RECEIVED = "com.courier.android.PUSH_RECEIVED"
        const val TOKEN_UPDATED = "com.courier.android.TOKEN_UPDATED"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            // Track + internal listeners
            Courier.shared.trackAndBroadcastTheEvent(
                trackingEvent = CourierTrackingEvent.DELIVERED,
                message = message
            )
            // Broadcast to the hosting app explicitly
            broadcastToHostApp(message)
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

    private fun broadcastToHostApp(message: RemoteMessage) {
        val payload = Bundle().apply {
            message.data.forEach { (k, v) -> putString(k, v) }
        }

        val intent = Intent(Events.PUSH_RECEIVED).apply {
            // Explicit to the same host app/package
            setPackage(applicationContext.packageName)
            // Flatten fields commonly used by apps
            putExtra("title", message.data["title"] ?: message.notification?.title)
            putExtra("body",  message.data["body"]  ?: message.notification?.body)
            putExtra("from",  message.from)
            putExtra("data",  payload)
            // Critical for killed state delivery (not force-stopped)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            // Hint to schedule sooner
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        // Single explicit broadcast is enough
        applicationContext.startService(intent)
    }
}