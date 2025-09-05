package com.courier.android.utils

import android.util.Log
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.CourierPushNotificationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationEventBus {

    companion object {
        private const val TAG = "EventBus"
    }

    private val _events = MutableSharedFlow<CourierPushNotificationEvent>()
    val events = _events.asSharedFlow()

    suspend fun onPushNotificationEvent(trackingEvent: CourierTrackingEvent, data: Map<String, String>) {
        Log.d(TAG, "onPushNotificationEvent: $trackingEvent = $data")
        val event = CourierPushNotificationEvent(trackingEvent, data)
        _events.emit(event)
    }

}