package com.courier.android.utils

import android.util.Log
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.CourierPushNotificationEvent
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationEventBus {

    companion object {
        private const val TAG = "EventBus"
    }

    private val _events = MutableSharedFlow<CourierPushNotificationEvent>()
    val events = _events.asSharedFlow()

    suspend fun onPushNotificationEvent(trackingEvent: CourierTrackingEvent, message: RemoteMessage) {
        Log.d(TAG, "onPushNotificationEvent: $trackingEvent = $message")
        val event = CourierPushNotificationEvent(trackingEvent, message)
        _events.emit(event)
    }

}