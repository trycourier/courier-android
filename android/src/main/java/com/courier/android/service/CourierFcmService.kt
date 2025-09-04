package com.courier.android.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract.Intents.Insert.ACTION
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.courier.android.Courier
import com.courier.android.activity.CourierActivity
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierMessage
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.modules.setFcmToken
import com.courier.android.notifications.present
import com.courier.android.notifications.presentNotification
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.courier.android.utils.toPushNotification
import com.courier.android.utils.trackAndBroadcastTheEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class CourierFcmService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()

        // Init the SDK if needed
        Courier.initialize(context = this)

    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        try {

            // Broadcast the message to the app
            // This will allow us to handle when it's delivered
            Courier.shared.trackAndBroadcastTheEvent(
                trackingEvent = CourierTrackingEvent.DELIVERED,
                message = message
            )

        } catch (e: Exception) {

            CourierClient.default.error(e.toString())

        }

        // Try and show the notification
//        val pushNotification = message.toPushNotification()

        val dataBundle = Bundle().apply {
            message.data.forEach { (k, v) ->
                putString(k, v)
            }
        }

        val test = dataBundle

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        try {

            Courier.shared.setFcmToken(
                token = token,
                onSuccess = { Courier.shared.client?.log("Courier FCM token updated") },
                onFailure = { Courier.shared.client?.error(it.toString()) }
            )

        } catch (e: Exception) {

            Courier.shared.client?.error(e.toString())

        }

    }

}