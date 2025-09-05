package com.courier.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.CallSuper
import com.courier.android.models.CourierMessage

abstract class CourierPushNotificationReceiver : BroadcastReceiver() {

    /** Override this to handle the parsed message */
    abstract fun onCourierMessage(context: Context, message: CourierMessage)

    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CourierFirebaseMessagingProxy.Events.PUSH_RECEIVED) {
            return
        }

        // Parse extras → CourierMessage (same logic as the service)
        val dataBundle = intent.getBundleExtra("data")
        val dataMap = dataBundle?.keySet()?.associateWith { key ->
            dataBundle.getString(key).orEmpty()
        }

        val message = CourierMessage(
            title = intent.getStringExtra("title"),
            body  = intent.getStringExtra("body"),
            data  = dataMap
        )

        // Handle the message
        onCourierMessage(context, message)
    }
}
