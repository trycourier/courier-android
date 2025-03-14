package com.courier.android.utils

import android.util.Log
import com.courier.android.models.InboxMessage
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PushNotificationEventBus {

    private val _events = MutableSharedFlow<RemoteMessage>()
    val events = _events.asSharedFlow()

    suspend fun emitEvent(message: RemoteMessage) {
        Log.d(TAG, "Emitting message = $message")
        _events.emit(message)
    }

    companion object {
        private const val TAG = "EventBus"
    }

}

internal class InboxSocketEventBus {

    private val _events = MutableSharedFlow<InboxMessage>()
    val events = _events.asSharedFlow()

    suspend fun emitEvent(message: InboxMessage) {
        Log.d(TAG, "Emitting message = $message")
        _events.emit(message)
    }

    companion object {
        private const val TAG = "EventBus"
    }

}