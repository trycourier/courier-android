package com.courier.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.courier.android.Courier
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.utils.broadcastPushNotification
import com.courier.android.utils.getRemoteMessage
import com.courier.android.utils.onPushNotificationEvent
import com.courier.android.utils.trackPushNotification
import com.courier.android.utils.trackingUrl
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

open class CourierActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Courier if needed
        Courier.initialize(context = this)

        // See if there is a pending click event
        checkIntentForPushNotificationClick(intent)

        // Handle delivered messages on the main thread
        Courier.shared.onPushNotificationEvent { event ->
            when (event.trackingEvent) {
                CourierTrackingEvent.CLICKED -> {
                    onPushNotificationClicked(event.remoteMessage)
                }
                CourierTrackingEvent.DELIVERED -> {
                    onPushNotificationDelivered(event.remoteMessage)
                }
                CourierTrackingEvent.OPENED,
                CourierTrackingEvent.READ,
                CourierTrackingEvent.UNREAD -> {
                    // no-op (intentionally ignored)
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntentForPushNotificationClick(intent)
    }

    private fun checkIntentForPushNotificationClick(intent: Intent?) {
        intent?.getRemoteMessage()?.let { remoteMessage ->
            CoroutineScope(Dispatchers.IO).launch {

                val trackingEvent = CourierTrackingEvent.CLICKED

                delay(3000) // 3000 ms

                // Broadcast the message
                Courier.shared.broadcastPushNotification(
                    trackingEvent = trackingEvent,
                    remoteMessage = remoteMessage
                )

                // Track the message
                remoteMessage.trackingUrl?.let { trackingUrl ->
                    Courier.shared.trackPushNotification(
                        trackingEvent = trackingEvent,
                        trackingUrl = trackingUrl
                    )
                }
            }
        }
    }

    open fun onPushNotificationClicked(remoteMessage: RemoteMessage) {}

    open fun onPushNotificationDelivered(remoteMessage: RemoteMessage) {}

}