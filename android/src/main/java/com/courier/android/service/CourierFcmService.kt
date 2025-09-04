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

object CourierEvents {
    // Intent action (explicit broadcasts still carry this for clarity)
    const val ACTION_COURIER_EVENT = "com.courier.android.ACTION_COURIER_EVENT"

    // Event types
    const val EVENT_MESSAGE_RECEIVED = "message_received"
    const val EVENT_TOKEN_REFRESHED  = "token_refreshed"

    // Extras
    const val EXTRA_EVENT  = "courier:event"      // one of the EVENT_* constants
    const val EXTRA_TITLE  = "courier:title"
    const val EXTRA_BODY   = "courier:body"
    const val EXTRA_DATA   = "courier:data"       // Bundle<String, String>
    const val EXTRA_TOKEN  = "courier:token"

    // App manifest <meta-data> key that points to the receiver FQCN
    const val MD_EVENT_RECEIVER = "com.courier.EVENT_RECEIVER"
}

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

        val dataBundle = Bundle().apply { message.data.forEach { (k, v) -> putString(k, v) } }

        broadcastToApp(
            context = this,
            event   = CourierEvents.EVENT_MESSAGE_RECEIVED,
            title   = message.data["title"] ?: message.notification?.title,
            body    = message.data["body"]  ?: message.notification?.body,
            data    = dataBundle
        )

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

        broadcastToApp(
            context = this,
            event   = CourierEvents.EVENT_TOKEN_REFRESHED,
            token   = token
        )

    }

    private fun broadcastToApp(
        context: Context,
        event: String,
        title: String? = null,
        body: String? = null,
        data: Bundle? = null,
        token: String? = null
    ) {
        val receiverClass = resolveReceiverClass(context) ?: return
        val intent = Intent(CourierEvents.ACTION_COURIER_EVENT).apply {
            component = ComponentName(context, receiverClass) // explicit → reliable in bg/killed
            putExtra(CourierEvents.EXTRA_EVENT, event)
            when (event) {
                CourierEvents.EVENT_MESSAGE_RECEIVED -> {
                    putExtra(CourierEvents.EXTRA_TITLE, title)
                    putExtra(CourierEvents.EXTRA_BODY,  body)
                    putExtra(CourierEvents.EXTRA_DATA,  data ?: Bundle())
                }
                CourierEvents.EVENT_TOKEN_REFRESHED -> {
                    putExtra(CourierEvents.EXTRA_TOKEN, token)
                }
            }
        }
        sendBroadcast(intent)
    }

    private fun broadcastToApp(event: String, extras: Intent) {
        val fqcn = readReceiverFqcn() ?: run {
            Log.w("CourierSDK", "No ${CourierEvents.MD_RECEIVER_FQCN} meta-data set; dropping event.")
            return
        }
        val cls = try { Class.forName(fqcn).asSubclass(BroadcastReceiver::class.java) }
        catch (t: Throwable) { Log.e("CourierSDK","Bad receiver FQCN: $fqcn", t); return }

        val intent = Intent(CourierEvents.ACTION).apply {
            component = ComponentName(this@CourierFcmService, cls) // explicit → reliable
            putExtra(CourierEvents.EXTRA_EVENT, event)
            putExtras(extras)
        }
        sendBroadcast(intent)
    }

    private fun readReceiverFqcn(): String? {
        val pm = packageManager
        val flags = if (Build.VERSION.SDK_INT >= 33)
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        else @Suppress("DEPRECATION") PackageManager.GET_META_DATA

        val appInfo = if (Build.VERSION.SDK_INT >= 33)
            pm.getApplicationInfo(packageName, flags)
        else @Suppress("DEPRECATION") pm.getApplicationInfo(packageName, flags as Int)

        return appInfo.metaData?.getString(CourierEvents.MD_RECEIVER_FQCN)
    }

}