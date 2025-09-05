package com.courier.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.utils.getPushNotificationData
import com.courier.android.utils.onPushNotificationEvent
import com.courier.android.utils.trackPushNotification
import com.courier.android.utils.trackingUrl
import kotlinx.coroutines.launch

open class CourierActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Courier if needed
        Courier.initialize(context = this)

        // Handle initial push
        checkIntentForPushNotificationClick(intent)

        // Handle delivered messages on the main thread
        Courier.shared.onPushNotificationEvent { event ->
            when (event.trackingEvent) {
                CourierTrackingEvent.DELIVERED -> {
                    onPushNotificationDelivered(event.data)
                }
                else -> Unit
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntentForPushNotificationClick(intent)
    }

    private fun checkIntentForPushNotificationClick(intent: Intent?) = lifecycleScope.launch {

        // Get the notification and tracking event
        val remoteMessage = intent?.getPushNotificationData() ?: return@launch
        val trackingEvent = CourierTrackingEvent.CLICKED

        // Broadcast the message
        onPushNotificationClicked(remoteMessage)

        // Track the message
        remoteMessage.trackingUrl?.let { trackingUrl ->
            Courier.shared.trackPushNotification(
                trackingEvent = trackingEvent,
                trackingUrl = trackingUrl
            )
        }

    }

    open fun onPushNotificationClicked(pushNotification: Map<String, String>) {}

    open fun onPushNotificationDelivered(pushNotification: Map<String, String>) {}

}