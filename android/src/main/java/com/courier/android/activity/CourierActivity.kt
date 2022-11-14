package com.courier.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.courier.android.Courier
import com.courier.android.trackPushNotificationClick
import com.google.firebase.messaging.RemoteMessage

open class CourierActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // See if there is a pending click event
        checkIntentForPushNotificationClick(intent)

        // Handle delivered messages on the main thread
        Courier.getLastDeliveredMessage { message ->
            onPushNotificationDelivered(message)
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIntentForPushNotificationClick(intent)
    }

    private fun checkIntentForPushNotificationClick(intent: Intent?) {
        intent?.trackPushNotificationClick { message ->
            onPushNotificationClicked(message)
        }
    }

    open fun onPushNotificationClicked(message: RemoteMessage) {}

    open fun onPushNotificationDelivered(message: RemoteMessage) {}

}