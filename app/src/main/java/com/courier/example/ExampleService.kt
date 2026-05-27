package com.courier.example

import com.courier.android.Courier
import com.courier.android.notifications.CourierPushNotificationIntent
import com.courier.android.notifications.presentNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// This is a working FirebaseMessagingService used by the example app.
// Two Courier SDK calls are required to integrate push tracking:
//   1. Courier.onMessageReceived(...)   — in onMessageReceived
//   2. Courier.onNewToken(...)          — in onNewToken
//
// IMPORTANT: Courier.onMessageReceived blocks until the tracking POST
// completes, so always post your notification *before* calling it.
// Otherwise the notification appears on screen only after the HTTP
// roundtrip finishes (up to ~8 s on a slow network).
//
// Docs:
//   Courier Android SDK    — https://www.courier.com/docs/sdk-libraries/android
//   Android notifications  — https://developer.android.com/develop/ui/views/notifications/build-notification
class ExampleService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // 1) Show the notification immediately so the user sees it without delay.
        val notificationIntent = CourierPushNotificationIntent(
            context = this,
            target = MainActivity::class.java,
            payload = message
        )

        notificationIntent.presentNotification(
            title = message.data["title"] ?: message.notification?.title,
            body = message.data["body"] ?: message.notification?.body,
        )

        // 2) Track the delivery. This call blocks until the POST completes (or
        //    times out) so the process survives long enough for the tracking HTTP
        //    call to land — important for killed-state delivery.
        Courier.onMessageReceived(message.data)

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Required — Syncs this device's FCM token with Courier.
        // Behind the scenes the SDK caches the token locally and uploads it to
        // Courier linked to the currently signed-in user. If no user is signed in
        // yet the token is held locally and synced on the next signIn() call.
        Courier.onNewToken(token)

    }

}
