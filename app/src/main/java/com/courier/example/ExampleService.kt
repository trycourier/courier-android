package com.courier.example

import com.courier.android.Courier
import com.courier.android.notifications.CourierPushNotificationIntent
import com.courier.android.notifications.presentNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExampleService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Notify the Courier SDK that a push was delivered
        Courier.onMessageReceived(message.data)

        // Create the PendingIntent that runs when the user taps the notification
        // This intent targets your Activity and carries the original message payload
        val intent = CourierPushNotificationIntent(
            context = this,
            target = MainActivity::class.java,
            payload = message
        )

        // Show the notification to the user.
        // Prefer data-only FCM so this service runs even in background/killed state.
        // Fall back to notification fields if data keys are missing.
        intent.presentNotification(
            title = message.data["title"] ?: message.notification?.title,
            body = message.data["body"] ?: message.notification?.body,
        )

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Register/refresh this device’s FCM token with Courier.
        // The SDK caches and updates the token automatically and links it to the current user.
        Courier.onNewToken(token)

    }

}