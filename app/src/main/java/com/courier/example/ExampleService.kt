package com.courier.example

import com.courier.android.models.CourierMessage
import com.courier.android.notifications.present
import com.courier.android.service.CourierService

class MyNotificationReceiver: CourierService() {

    override fun onCourierMessage(message: CourierMessage) {
        message.present(this, MainActivity::class.java)
    }

}