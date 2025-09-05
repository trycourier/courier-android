package com.courier.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierMessage
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFcmToken
import com.courier.android.utils.trackAndBroadcastTheEvent
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.courier.android.utils.toPushNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

abstract class CourierReceiver : BroadcastReceiver() {

    final override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CourierActions.MESSAGE_RECEIVED -> {
                val courierMessage = CourierMessage(
                    title = intent.getStringExtra("title"),
                    body = intent.getStringExtra("body"),
//                    data = intent.getStringExtra("data")
                )

                onCourierMessage(context, courierMessage)
            }
        }
    }

    // Developer just implements this
    abstract fun onCourierMessage(context: Context, message: CourierMessage)
}