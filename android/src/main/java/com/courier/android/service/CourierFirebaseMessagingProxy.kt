package com.courier.android.service

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
     * Dynamically discovers and starts CourierService implementations in the host app.
     * This approach works in all app states including killed state by using explicit intents
     * with component names, avoiding the implicit intent limitations of modern Android.
     */
    private fun broadcastToHostApp(message: RemoteMessage) {
        val payload = Bundle().apply {
            message.data.forEach { (k, v) -> putString(k, v) }
        }

        // Find all services in the app that can handle our action
        val courierServices = findCourierServices()
        
        if (courierServices.isEmpty()) {
            CourierClient.default.error("No CourierService found in app manifest. Please ensure your service extends CourierService and has the correct intent filter.")
            return
        }

        // Create explicit intents for each discovered service
        courierServices.forEach { serviceInfo ->
            try {
                val explicitIntent = Intent().apply {
                    // Explicit component targeting - this is the key for killed state support
                    component = ComponentName(serviceInfo.serviceInfo.packageName, serviceInfo.serviceInfo.name)
                    action = Events.PUSH_RECEIVED
                    putExtra("title", message.data["title"] ?: message.notification?.title)
                    putExtra("body", message.data["body"] ?: message.notification?.body)
                    putExtra("from", message.from)
                    putExtra("data", payload)
                    // Critical for killed state delivery (not force-stopped)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }

                // Use explicit component name - this works in killed state
                applicationContext.startService(explicitIntent)
                
            } catch (e: SecurityException) {
                CourierClient.default.error("Failed to start service ${serviceInfo.serviceInfo.name}: ${e.message}")
            } catch (e: Exception) {
                CourierClient.default.error("Error starting service ${serviceInfo.serviceInfo.name}: ${e.message}")
            }
        }
    }

    /**
     * Dynamically discovers services in the host app that can handle MESSAGE_RECEIVED intents.
     * This uses PackageManager to query services with the appropriate intent filter,
     * ensuring we don't need the developer to provide package names manually.
     */
    private fun findCourierServices(): List<ResolveInfo> {
        return try {
            val queryIntent = Intent(Events.PUSH_RECEIVED)
            val packageManager = applicationContext.packageManager
            
            // Debug info
            CourierClient.default.log("Searching for action: ${Events.PUSH_RECEIVED}")
            CourierClient.default.log("Our package name: ${applicationContext.packageName}")
            
            // Query services that can handle our action - use 0 flags to find all matching services
            val allServices = packageManager.queryIntentServices(queryIntent, 0)
            
            CourierClient.default.log("Total services found for action: ${allServices.size}")
            allServices.forEach { resolveInfo ->
                CourierClient.default.log("Service: ${resolveInfo.serviceInfo.name} in package: ${resolveInfo.serviceInfo.packageName}")
            }
            
            // Also try with different flags to see if that makes a difference
            val servicesWithDefault = packageManager.queryIntentServices(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)
            CourierClient.default.log("Services with MATCH_DEFAULT_ONLY: ${servicesWithDefault.size}")
            
            val servicesWithAll = packageManager.queryIntentServices(queryIntent, PackageManager.MATCH_ALL)
            CourierClient.default.log("Services with MATCH_ALL: ${servicesWithAll.size}")
            
            // Filter to only include services from our own package
            val services = allServices.filter { resolveInfo ->
                val isOurPackage = resolveInfo.serviceInfo.packageName == applicationContext.packageName
                val isEnabled = resolveInfo.serviceInfo.enabled
                
                CourierClient.default.log("Service ${resolveInfo.serviceInfo.name}: ourPackage=$isOurPackage, enabled=$isEnabled")
                
                isOurPackage && isEnabled
            }
            
            CourierClient.default.log("Found ${services.size} CourierService(s) in our app package")
            services
        } catch (e: Exception) {
            CourierClient.default.error("Failed to query CourierServices: ${e.message}")
            emptyList()
        }
    }
}