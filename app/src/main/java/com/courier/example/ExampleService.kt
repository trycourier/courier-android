package com.courier.example

import android.annotation.SuppressLint
import com.courier.android.notifications.presentNotification
import com.courier.android.service.CourierService
import com.google.firebase.messaging.RemoteMessage

// Warning is suppressed
// You do not need to worry about this warning
// The CourierService will handle the function automatically
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class ExampleService: CourierService() {

    override fun showNotification(message: RemoteMessage) {
        super.showNotification(message)

        // This is a simple function you can use, however,
        // it is recommended that you create your own
        // notification and handle it's intent properly
        message.presentNotification(
            context = this,
            handlingClass = MainActivity::class.java,
            icon = android.R.drawable.ic_dialog_info
        )

        // Courier will handle delivery tracking of notifications automatically
        // if you extend your service class with `CourierService()`
        // If you do present a custom notification, you should use this
        // function when the notification is clicked to ensure the status is updated properly
        /**
        Courier.trackNotification(
            message = message,
            event = CourierPushEvent.DELIVERED or CourierPushEvent.CLICKED,
            onSuccess = { Courier.log("Event tracked") },
            onFailure = { Courier.log(it.toString()) }
        )
        **/

    }

}