package com.courier.android.utils

import com.courier.android.Courier
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.CourierPushNotificationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationEventBus {

    private val _events = MutableSharedFlow<CourierPushNotificationEvent>()
    val events = _events.asSharedFlow()

    suspend fun onPushNotificationEvent(trackingEvent: CourierTrackingEvent, data: Map<String, String>) {
        Courier.shared.client?.log("onPushNotificationEvent: $trackingEvent = $data")
        val event = CourierPushNotificationEvent(trackingEvent, data)
        _events.emit(event)
    }

}