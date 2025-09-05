package com.courier.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import com.courier.android.models.CourierMessage

abstract class CourierService : Service() {

    /** Override this to handle the parsed message */
    abstract fun onCourierMessage(message: CourierMessage)

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != CourierFirebaseProxy.Events.MESSAGE_RECEIVED) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        // Parse extras → CourierMessage (same logic you had)
        val dataBundle = intent.getBundleExtra("data")
        val dataMap = dataBundle?.keySet()?.associateWith { key ->
            dataBundle.getString(key).orEmpty()
        }

        val message = CourierMessage(
            title = intent.getStringExtra("title"),
            body  = intent.getStringExtra("body"),
            data  = dataMap
        )

        // Do work off the main thread; stop service when done
        onCourierMessage(message)

        // We don’t need to be restarted if killed
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

}