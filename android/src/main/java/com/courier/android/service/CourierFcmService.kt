package com.courier.android.service

import com.courier.android.Courier
import com.courier.android.models.CourierMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal class CourierFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {

        val title = message.data["title"] ?: message.notification?.title ?: "Empty Title"
        val body = message.data["body"] ?: message.notification?.body ?: "Empty Body"

        val cm = CourierMessage(
            title = title,
            body  = body,
            data  = message.data
        )

        Courier.internalHandler?.onDelivered(cm)
        Courier.internalHandler?.showNotification(cm)

    }

    override fun onNewToken(token: String) {
        Courier.internalHandler?.onToken(token)
    }

}