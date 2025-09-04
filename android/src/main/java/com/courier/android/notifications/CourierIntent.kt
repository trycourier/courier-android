package com.courier.android.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.courier.android.Courier
import com.courier.android.models.CourierMessage
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

internal class CourierIntent(private val context: Context, cls: Class<*>?, message: RemoteMessage) : Intent(context, cls) {

    init {
        putExtra(Courier.COURIER_PENDING_NOTIFICATION_KEY, message)
        addCategory(CATEGORY_LAUNCHER)
        addFlags(FLAG_ACTIVITY_SINGLE_TOP)
        action = ACTION_MAIN
    }

    internal val pendingIntent get() = PendingIntent.getActivity(
        context,
        0,
        this,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
    )

}

internal class CourierPushNotificationIntent(private val context: Context, cls: Class<*>?, message: CourierMessage) : Intent(context, cls) {

    init {
        putExtra(Courier.COURIER_PENDING_NOTIFICATION_KEY, Gson().toJson(message))
        addCategory(CATEGORY_LAUNCHER)
        addFlags(FLAG_ACTIVITY_SINGLE_TOP)
        action = ACTION_MAIN
    }

    internal val pendingIntent get() = PendingIntent.getActivity(
        context,
        0,
        this,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
    )

}