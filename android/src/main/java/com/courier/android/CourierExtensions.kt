package com.courier.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.courier.android.Courier.Companion.COURIER_COROUTINE_CONTEXT
import com.courier.android.Courier.Companion.eventBus
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierPushEvent
import com.courier.android.repositories.MessagingRepository
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal fun Courier.Companion.log(data: String) {
    if (shared.isDebugging) {
        Log.d(TAG, data)
        shared.logListener?.invoke(data)
    }
}

fun Intent.trackPushNotificationClick(onClick: (message: RemoteMessage) -> Unit) {

    try {

        // Check to see if we have an intent to work
        val key = Courier.COURIER_PENDING_NOTIFICATION_KEY
        (extras?.get(key) as? RemoteMessage)?.let { message ->

            // Clear the intent extra
            extras?.remove(key)

            // Track when the notification was clicked
            Courier.shared.trackNotification(
                message = message,
                event = CourierPushEvent.CLICKED,
                onSuccess = { Courier.log("Event tracked") },
                onFailure = { Courier.log(it.toString()) }
            )

            onClick(message)

        }

    } catch (e: Exception) {

        Courier.log(e.toString())

    }

}

/**
 *
 * Sends a test push to a user id you provider.
 * It is not recommended to keep this in your production app for authKey security reasons.
 *
 * @param isProduction used for APNS (Apple Push Notification Service)
 * to declare with entitlement environment to use
 * For FCM (Firebase Cloud Messaging) you do not need to worry about this
 *
 */
suspend fun Courier.sendPush(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider> = CourierProvider.values().toList(), isProduction: Boolean): String {
    return MessagingRepository().send(
        authKey = authKey,
        userId = userId,
        title = title,
        body = body,
        providers = providers,
        isProduction = isProduction
    )
}

fun Courier.sendPush(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider> = CourierProvider.values().toList(), isProduction: Boolean, onSuccess: (requestId: String) -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        val messageId = Courier.shared.sendPush(
            authKey = authKey,
            userId = userId,
            title = title,
            body = body,
            providers = providers,
            isProduction = isProduction,
        )
        onSuccess(messageId)
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun Courier.trackNotification(message: RemoteMessage, event: CourierPushEvent) = withContext(COURIER_COROUTINE_CONTEXT) {
    val trackingUrl = message.data["trackingUrl"] ?: return@withContext
    MessagingRepository().postTrackingUrl(
        url = trackingUrl,
        event = event
    )
}

fun Courier.trackNotification(message: RemoteMessage, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        Courier.shared.trackNotification(
            message = message,
            event = event
        )
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun Courier.trackNotification(trackingUrl: String, event: CourierPushEvent) = withContext(COURIER_COROUTINE_CONTEXT) {
    MessagingRepository().postTrackingUrl(
        url = trackingUrl,
        event = event
    )
}

fun Courier.trackNotification(trackingUrl: String, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        Courier.shared.trackNotification(
            trackingUrl = trackingUrl,
            event = event
        )
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

internal fun Courier.broadcastMessage(message: RemoteMessage) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        eventBus.emitEvent(message)
    } catch (e: Exception) {
        Courier.log(e.toString())
    }
}

fun AppCompatActivity.requestNotificationPermission(onStatusChange: (granted: Boolean) -> Unit) {

    // Check if the notification manager can show push notifications
    val notificationManagerCompat = NotificationManagerCompat.from(this)
    val areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled()

    // Handle granting notification permission if possible
    if (Build.VERSION.SDK_INT >= 33) {
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val canReceivePushes = granted && areNotificationsEnabled
            onStatusChange(canReceivePushes)
        }
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        onStatusChange(areNotificationsEnabled)
    }

}

suspend fun AppCompatActivity.requestNotificationPermission() = suspendCoroutine { continuation ->
    requestNotificationPermission { granted ->
        continuation.resume(granted)
    }
}

fun AppCompatActivity.getNotificationPermissionStatus(onStatusChange: (granted: Boolean) -> Unit) {
    val notificationManagerCompat = NotificationManagerCompat.from(this)
    val areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled()
    onStatusChange(areNotificationsEnabled)
}

suspend fun AppCompatActivity.getNotificationPermissionStatus() = suspendCoroutine { continuation ->
    getNotificationPermissionStatus { granted ->
        continuation.resume(granted)
    }
}

val RemoteMessage.pushNotification: Map<String, Any?>
    get() {

        val rawData = data.toMutableMap()
        val payload = mutableMapOf<String, Any?>()

        // Add existing values to base map
        // then remove the unneeded keys
        val baseKeys = listOf("title", "subtitle", "body", "badge", "sound")
        baseKeys.forEach { key ->
            payload[key] = data[key]
            rawData.remove(key)
        }

        // Add extras
        for ((key, value) in rawData) {
            payload[key] = value
        }

        // Add the raw data
        payload["raw"] = data

        return payload

    }