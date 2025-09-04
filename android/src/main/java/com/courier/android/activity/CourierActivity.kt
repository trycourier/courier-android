package com.courier.android.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.courier.android.Courier
import com.courier.android.models.CourierMessage
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.utils.onPushNotificationEvent
import com.courier.android.utils.toPushNotification
import com.courier.android.utils.trackPushNotificationClick

open class CourierActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init Courier if needed
        Courier.initialize(context = this)

        // See if there is a pending click event
        checkIntentForPushNotificationClick(intent)

        // Handle delivered messages on the main thread
        Courier.shared.onPushNotificationEvent { event ->
            if (event.trackingEvent == CourierTrackingEvent.DELIVERED) {
                val pushNotification = event.remoteMessage.toPushNotification()
                onPushNotificationDelivered(pushNotification)
            }
        }

        // Set the current push handler activity
        // This is used by the default notification presentation to handle clicks
        // Developers can override this if needed
        Courier.shared.pushHandlerActivity = this::class.java

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntentForPushNotificationClick(intent)
    }

    private fun checkIntentForPushNotificationClick(intent: Intent?) {
        intent?.trackPushNotificationClick { message ->
            val pushNotification = message.toPushNotification()
            onPushNotificationClicked(pushNotification)
        }
    }

    open fun onPushNotificationClicked(message: CourierMessage) {}

    open fun onPushNotificationDelivered(message: CourierMessage) {}

}