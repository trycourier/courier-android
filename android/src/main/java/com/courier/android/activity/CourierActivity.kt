package com.courier.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.courier.android.Courier
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.PushNotification
import com.courier.android.utils.onPushNotificationEvent
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
                onPushNotificationDelivered(event.pushNotification)
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIntentForPushNotificationClick(intent)
    }

    private fun checkIntentForPushNotificationClick(intent: Intent?) {
        intent?.trackPushNotificationClick { pushNotification ->
            onPushNotificationClicked(pushNotification)
        }
    }

    open fun onPushNotificationClicked(pushNotification: PushNotification) {}

    open fun onPushNotificationDelivered(pushNotification: PushNotification) {}

}