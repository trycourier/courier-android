package com.courier.android

import android.util.Log
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierPushEvent
import com.courier.android.repositories.MessagingRepository
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun Courier.Companion.log(data: String) {
    if (instance.isDebugging) {
        Log.d(TAG, data)
    }
}

suspend fun Courier.Companion.sendPush(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider> = CourierProvider.values().toList()): String {
    return MessagingRepository().send(
        authKey = authKey,
        userId = userId,
        title = title,
        body = body,
        providers = providers,
        isProduction = !BuildConfig.DEBUG
    )
}

fun Courier.Companion.sendPush(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider> = CourierProvider.values().toList(), onSuccess: (requestId: String) -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
    try {
        val messageId = Courier.sendPush(
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

suspend fun Courier.Companion.trackNotification(message: RemoteMessage, event: CourierPushEvent) = withContext(COURIER_COROUTINE_CONTEXT) {
    val trackingUrl = message.data["trackingUrl"] ?: return@withContext
    MessagingRepository().postTrackingUrl(
        url = trackingUrl,
        event = event
    )
}

fun Courier.Companion.trackNotification(message: RemoteMessage, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
    try {
        Courier.trackNotification(
            message = message,
            event = event
        )
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

internal fun Courier.Companion.broadcastMessage(message: RemoteMessage) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
    try {
        eventBus.emitEvent(message)
    } catch (e: Exception) {
        Courier.log(e.toString())
    }
}