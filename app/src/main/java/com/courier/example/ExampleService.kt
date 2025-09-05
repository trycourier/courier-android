package com.courier.example

import android.content.Context
import com.courier.android.models.CourierMessage
import com.courier.android.notifications.present
import com.courier.android.service.CourierPushNotificationReceiver

class CustomCourierPushNotificationReceiver: CourierPushNotificationReceiver() {

    override fun onCourierMessage(context: Context, message: CourierMessage) {
        message.present(context, MainActivity::class.java)
    }

}