package com.courier.android.utils

import android.util.Log
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.CourierPushNotificationEvent
import com.courier.android.models.PushNotification
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationEventBus {

    companion object {
        private const val TAG = "EventBus"
    }

    private val _events = MutableSharedFlow<CourierPushNotificationEvent>()
    val events = _events.asSharedFlow()

    suspend fun onPushNotificationEvent(trackingEvent: CourierTrackingEvent, pushNotification: PushNotification) {
        Log.d(TAG, "onPushNotificationEvent: $trackingEvent = $pushNotification")
        val event = CourierPushNotificationEvent(trackingEvent, pushNotification)
        _events.emit(event)
    }

}