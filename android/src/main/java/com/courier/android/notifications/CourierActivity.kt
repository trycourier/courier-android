package com.courier.android.notifications

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.courier.android.Courier
import com.courier.android.detectPushNotificationClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class CourierActivity : AppCompatActivity() {

    var pushNotificationCallbacks: CourierPushNotificationCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // See if there is a pending click event
        intent.detectPushNotificationClick { message ->
            pushNotificationCallbacks?.onPushNotificationClicked(message)
        }

        // Handle delivered messages on the main thread
        handlePushDeliveredEvent()

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.detectPushNotificationClick { message ->
            pushNotificationCallbacks?.onPushNotificationClicked(message)
        }
    }

    private fun handlePushDeliveredEvent() {
        CoroutineScope(Courier.COURIER_COROUTINE_CONTEXT).launch(Dispatchers.Main) {
            Courier.eventBus.events.collectLatest { message ->
                pushNotificationCallbacks?.onPushNotificationDelivered(message)
            }
        }
    }

}