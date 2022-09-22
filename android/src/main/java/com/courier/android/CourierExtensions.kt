package com.courier.android

import android.Manifest
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal fun Courier.Companion.log(data: String) {
    if (shared.isDebugging) {
        Log.d(TAG, data)
    }
}

internal fun Courier.Companion.warn(data: String) {
    if (shared.isDebugging) {
        Log.e(TAG, data)
    }
}

suspend fun Courier.sendPush(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider> = CourierProvider.values().toList()): String {
    return MessagingRepository().send(
        authKey = authKey,
        userId = userId,
        title = title,
        body = body,
        providers = providers,
        isProduction = !BuildConfig.DEBUG
    )
}

fun Courier.sendPush(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider> = CourierProvider.values().toList(), onSuccess: (requestId: String) -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
    try {
        val messageId = Courier.shared.sendPush(
            authKey = authKey,
            userId = userId,
            title = title,
            body = body,
            providers = providers
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

fun Courier.trackNotification(message: RemoteMessage, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
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

fun Courier.trackNotification(trackingUrl: String, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
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

internal fun Courier.broadcastMessage(message: RemoteMessage) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
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

    onStatusChange(areNotificationsEnabled)

//    TODO: Add support after React Native and Flutter SDKs
//    // Handle granting notification permission if possible
//    if (Build.VERSION.SDK_INT >= 33) {
//        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            val canReceivePushes = granted && areNotificationsEnabled
//            onStatusChange(canReceivePushes)
//        }
//        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//    } else {
//        onStatusChange(areNotificationsEnabled)
//    }

}

suspend fun AppCompatActivity.requestNotificationPermission() = suspendCoroutine { continuation ->
    requestNotificationPermission { granted ->
        continuation.resume(granted)
    }
}