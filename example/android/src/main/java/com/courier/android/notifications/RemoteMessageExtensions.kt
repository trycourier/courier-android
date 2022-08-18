package com.courier.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage

fun RemoteMessage.presentNotification(context: Context, handlingClass: Class<*>?, icon: Int) {

    try {

        val channelId = "default"
        val pendingIntent = CourierIntent(context, handlingClass, this).pendingIntent
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(data["title"].orEmpty())
            .setContentText(data["body"].orEmpty())
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel human readable title", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val uuid = System.currentTimeMillis().toInt()
        notificationManager.notify(uuid, notificationBuilder.build())

    } catch (e: Exception) {

        print(e)

    }

}