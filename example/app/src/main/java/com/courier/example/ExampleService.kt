package com.courier.example

import com.courier.android.service.CourierService
import com.courier.android.notifications.presentNotification
import com.google.firebase.messaging.RemoteMessage

class ExampleService: CourierService() {

    override fun showNotification(message: RemoteMessage) {
        super.showNotification(message)

        // This is how you want to present the notification
        // You can use your own function here to present the notification
        // or use the one provided by Courier
        message.presentNotification(
            context = this,
            handlingClass = MainActivity::class.java
        )

    }

}