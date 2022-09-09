package com.courier.android.notifications

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.courier.android.Courier
import com.courier.android.log
import com.courier.android.models.CourierPushEvent
import com.courier.android.trackNotification
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class CourierActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // See if there is a pending click event
        intent.detectPushNotificationClick()

        // Handle delivered messages on the main thread
        handlePushDeliveredEvent()

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.detectPushNotificationClick()
    }

    private fun Intent.detectPushNotificationClick() {

        // Check to see if we have an intent to work with
        val key = Courier.COURIER_PENDING_NOTIFICATION_KEY
        (extras?.get(key) as? RemoteMessage)?.let { message ->

            // Clear the intent extra
            extras?.remove(key)

            // Track when the notification was clicked
            Courier.shared.trackNotification(
                message = message,
                event = CourierPushEvent.CLICKED,
                onSuccess = { Courier.log("Event tracked") },
                onFailure = { Courier.log(it.toString()) }
            )

            onPushNotificationClicked(message)

        }

    }

    private fun handlePushDeliveredEvent() {
        CoroutineScope(Courier.COURIER_COROUTINE_CONTEXT).launch(Dispatchers.Main) {
            Courier.eventBus.events.collectLatest { message ->
                onPushNotificationDelivered(message)
            }
        }
    }

    open fun onPushNotificationClicked(message: RemoteMessage) {}

    open fun onPushNotificationDelivered(message: RemoteMessage) {}

}