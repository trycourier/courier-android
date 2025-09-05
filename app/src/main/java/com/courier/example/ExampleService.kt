package com.courier.example

import com.courier.android.Courier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExampleService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Courier.onMessageReceived(message)
        Courier.presentNotification(message, this, MainActivity::class.java)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Courier.onNewToken(token)
    }

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

}