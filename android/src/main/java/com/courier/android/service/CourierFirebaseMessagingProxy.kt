package com.courier.android.service

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import com.courier.android.Courier
import com.courier.android.activity.CourierActivity
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFcmToken
import com.courier.android.notifications.presentNotification
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.courier.android.utils.trackAndBroadcastTheEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class CourierFirebaseMessagingProxy : FirebaseMessagingService() {

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

    /**
     * Dynamically discovers and sends broadcasts to CourierPushNotificationReceiver implementations in the host app.
     * This approach works in all app states including killed state by using broadcast receivers
     * instead of services, which are not restricted by Android's background execution limits.
     */
    private fun broadcastToHostApp(message: RemoteMessage) {
        val payload = Bundle().apply {
            message.data.forEach { (k, v) -> putString(k, v) }
        }

        // Find all receivers in the app that can handle our action
        val courierReceivers = findCourierReceivers()
        
        if (courierReceivers.isEmpty()) {
            CourierClient.default.error("No CourierPushNotificationReceiver found in app manifest. Please ensure your receiver extends CourierPushNotificationReceiver and has the correct intent filter.")
            return
        }

        // Create explicit intents for each discovered receiver
        courierReceivers.forEach { receiverInfo ->
            try {
                val explicitIntent = Intent().apply {
                    // Explicit component targeting - this works for broadcast receivers in killed state
                    component = ComponentName(receiverInfo.activityInfo.packageName, receiverInfo.activityInfo.name)
                    action = Events.PUSH_RECEIVED
                    putExtra("title", message.data["title"] ?: message.notification?.title)
                    putExtra("body", message.data["body"] ?: message.notification?.body)
                    putExtra("from", message.from)
                    putExtra("data", payload)
                    // Critical for killed state delivery (not force-stopped)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }

                // Use explicit broadcast - this works in killed state
                applicationContext.sendBroadcast(explicitIntent)
                
            } catch (e: SecurityException) {
                CourierClient.default.error("Failed to send broadcast to ${receiverInfo.activityInfo.name}: ${e.message}")
            } catch (e: Exception) {
                CourierClient.default.error("Error sending broadcast to ${receiverInfo.activityInfo.name}: ${e.message}")
            }
        }
    }

    /**
     * Dynamically discovers broadcast receivers in the host app that can handle PUSH_RECEIVED intents.
     * This uses PackageManager to query receivers with the appropriate intent filter,
     * ensuring we don't need the developer to provide package names manually.
     */
    private fun findCourierReceivers(): List<ResolveInfo> {
        return try {
            val queryIntent = Intent(Events.PUSH_RECEIVED)
            val packageManager = applicationContext.packageManager
            
            // Debug info
            CourierClient.default.log("Searching for broadcast receivers with action: ${Events.PUSH_RECEIVED}")
            CourierClient.default.log("Our package name: ${applicationContext.packageName}")
            
            // Query broadcast receivers that can handle our action - use 0 flags to find all matching receivers
            val allReceivers = packageManager.queryBroadcastReceivers(queryIntent, 0)
            
            CourierClient.default.log("Total receivers found for action: ${allReceivers.size}")
            allReceivers.forEach { resolveInfo ->
                CourierClient.default.log("Receiver: ${resolveInfo.activityInfo.name} in package: ${resolveInfo.activityInfo.packageName}")
            }
            
            // Also try with different flags to see if that makes a difference
            val receiversWithDefault = packageManager.queryBroadcastReceivers(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)
            CourierClient.default.log("Receivers with MATCH_DEFAULT_ONLY: ${receiversWithDefault.size}")
            
            val receiversWithAll = packageManager.queryBroadcastReceivers(queryIntent, PackageManager.MATCH_ALL)
            CourierClient.default.log("Receivers with MATCH_ALL: ${receiversWithAll.size}")
            
            // Filter to only include receivers from our own package
            val receivers = allReceivers.filter { resolveInfo ->
                val isOurPackage = resolveInfo.activityInfo.packageName == applicationContext.packageName
                val isEnabled = resolveInfo.activityInfo.enabled
                
                CourierClient.default.log("Receiver ${resolveInfo.activityInfo.name}: ourPackage=$isOurPackage, enabled=$isEnabled")
                
                isOurPackage && isEnabled
            }
            
            CourierClient.default.log("Found ${receivers.size} CourierPushNotificationReceiver(s) in our app package")
            receivers
        } catch (e: Exception) {
            CourierClient.default.error("Failed to query CourierPushNotificationReceivers: ${e.message}")
            emptyList()
        }
    }
}