package com.courier.example

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.courier.android.models.CourierMessage
import com.courier.android.notifications.present
import com.courier.android.notifications.presentNotification
import com.courier.android.service.CourierReceiver
//import com.courier.android.service.CourierService
//import com.google.firebase.messaging.RemoteMessage

// Warning is suppressed
// You do not need to worry about this warning
// The CourierService will handle the function automatically
//@SuppressLint("MissingFirebaseInstanceTokenRefresh")
//class ExampleService: CourierService() {
//
//    override fun showNotification(message: RemoteMessage) {
//        super.showNotification(message)
//
//        // This is a simple function you can use, however,
//        // it is recommended that you create your own
//        // notification and handle it's intent properly
//        message.presentNotification(
//            context = this,
//            handlingClass = MainActivity::class.java,
//        )
//
//        // Courier will handle delivery tracking of notifications automatically if you extend your service class with `CourierService()`
//        // If you do present a custom notification, you should use this function when the notification is clicked to ensure the status is updated properly
//        /**
//        Courier.trackNotification(
//            message = message,
//            event = CourierPushEvent.DELIVERED,
//            onSuccess = { Courier.log("Event tracked") },
//            onFailure = { Courier.log(it.toString()) }
//        )
//        **/
//
//    }
//
//}

//// App module (no Firebase imports)
//class MyCourierReceiver : CourierNotificationBroadcast() {
//
//    override fun onCourierMessage(
//        context: Context,
//        title: String?,
//        body: String?,
//        data: Map<String, String>
//    ) {
//        // Example: post your user-visible notification
//        showNotification(context, title ?: "New message", body.orEmpty(), data)
//    }
//
//    override fun onCourierNewToken(context: Context, token: String) {
//        // Upload token to backend, logging, etc.
//    }
//
//    private fun showNotification(
//        ctx: Context,
//        title: String,
//        body: String,
//        data: Map<String, String>
//    ) {
//        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
//        val channelId = "my-courier"
//        if (android.os.Build.VERSION.SDK_INT >= 26 &&
//            nm.getNotificationChannel(channelId) == null
//        ) {
//            nm.createNotificationChannel(
//                android.app.NotificationChannel(
//                    channelId, "Courier",
//                    android.app.NotificationManager.IMPORTANCE_HIGH
//                )
//            )
//        }
//
//        val tap = android.app.PendingIntent.getActivity(
//            ctx,
//            0,
//            Intent(ctx, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//                putExtra("courier_data", HashMap(data))
//            },
//            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notif = androidx.core.app.NotificationCompat.Builder(ctx, channelId)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle(title)
//            .setContentText(body)
//            .setAutoCancel(true)
//            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(tap)
//            .build()
//
//        nm.notify(System.currentTimeMillis().toInt(), notif)
//    }
//}

class MyNotificationReceiver: CourierReceiver() {

    override fun onCourierMessage(context: Context, message: CourierMessage) {
        message.present(context, MainActivity::class.java)
    }

}