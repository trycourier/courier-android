package com.courier.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

abstract class CourierNotificationBroadcast : BroadcastReceiver() {

    /** Override to handle new messages */
    protected open fun onCourierMessage(
        context: Context,
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {}

    /** Override to handle new/rotated FCM tokens */
    protected open fun onCourierNewToken(
        context: Context,
        token: String
    ) {}

    final override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CourierEvents.ACTION_COURIER_EVENT) return

        when (intent.getStringExtra(CourierEvents.EXTRA_EVENT)) {
            CourierEvents.EVENT_MESSAGE_RECEIVED -> {
                val title  = intent.getStringExtra(CourierEvents.EXTRA_TITLE)
                val body   = intent.getStringExtra(CourierEvents.EXTRA_BODY)
                val bundle = intent.getBundleExtra(CourierEvents.EXTRA_DATA) ?: Bundle()
                val data   = bundle.keySet().associateWith { bundle.getString(it).orEmpty() }
                onCourierMessage(context, title, body, data)
            }
            CourierEvents.EVENT_TOKEN_REFRESHED -> {
                val token = intent.getStringExtra(CourierEvents.EXTRA_TOKEN) ?: return
                onCourierNewToken(context, token)
            }
        }
    }
}