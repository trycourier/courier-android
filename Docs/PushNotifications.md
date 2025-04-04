<img width="1000" alt="android-push-banner" src="https://github.com/trycourier/courier-android/assets/6370613/72159318-47d8-4d2c-ab21-30d8e8b78dea">

&emsp;

# Push Notifications

The easiest way to support push notifications in your app.

## Features

<table>
    <thead>
        <tr>
            <th width="300px" align="left">Feature</th>
            <th width="700px" align="left">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#automatic-token-syncing-fcm-only">
                    <code>Automatic Token Management</code>
                </a>
            </td>
            <td align="left">
                Skip manually managing push notification device tokens.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#manual-token-syncing">
                    <code>Manual Token Managment</code>
                </a>
            </td>
            <td align="left">
                Sync push notification tokens into Courier
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#manual-notification-tracking">
                    <code>Manual Notification Tracking</code>
                </a>
            </td>
            <td align="left">
                Track if your users are receiving your notifications even if your app is not runnning or open.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#5-register-for-notifications">
                    <code>Permission Requests & Checking</code>
                </a>
            </td>
            <td align="left">
                Simple functions to request and check push notification permission settings.
            </td>
        </tr>
    </tbody>
</table>

&emsp;

## Requirements

<table>
    <thead>
        <tr>
            <th width="300px" align="left">Requirement</th>
            <th width="700px" align="left">Reason</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://firebase.google.com/">
                    <code>Firebase Account</code>
                </a>
            </td>
            <td align="left">
                Needed to send push notifications out to your Android devices. Courier recommends you do this for the most ideal developer experience.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels">
                    <code>A Configured Provider</code>
                </a>
            </td>
            <td align="left">
                Courier needs to know who to route the push notifications to so your users can receive them.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Authentication.md">
                    <code>Authentication</code>
                </a>
            </td>
            <td align="left">
                Needs Authentication to sync push notification device tokens to the current user and Courier.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                A phyical Android device
            </td>
            <td align="left">
                Although you can setup the Courier SDK without a physical device, a physical device is the best way to fully ensure push notification tokens and notification delivery is working correctly. Simulators are not reliable.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                Release Mode Enabled
            </td>
            <td align="left">
                To confirm notifications are working, run your app in release mode. You can use debug mode for testing, but release mode is recommended.
            </td>
        </tr>
    </tbody>
</table>

&emsp;

# Setup 

## 1. Setup a Push Notification Provider

Select which push notification provider you would like Courier to route push notifications to. Choose APNS - Apple Push Notification Service if you are not sure which provider to use.

<table>
    <thead>
        <tr>
            <th width="850px" align="left">Provider</th>
            <th width="200px" align="center">Token Syncing</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels/firebase-fcm">
                    <code>(FCM) - Firebase Cloud Messaging</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#automatic-token-syncing-fcm-only">
                     <code>Automatic</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels/expo">
                    <code>Expo</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#manual-token-syncing">
                    <code>Manual</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels/onesignal">
                    <code>OneSignal</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#manual-token-syncing">
                    <code>Manual</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels/pusher-beams">
                    <code>Pusher Beams</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md#manual-token-syncing">
                    <code>Manual</code>
                </a>
            </td>
        </tr>
    </tbody>
</table>

⚠️ If you are using Firebase Cloud Messaging, please make sure you initialize the Firebase SDK before continuing. Follow this [`Setup Guide`](https://firebase.google.com/docs/android/setup) for Firebase on Android.

&emsp;

## 2. Sync Push Notification Tokens

### Automatic Token Syncing (FCM Only)

To track new push notifications when they arrive and to automatically sync push notification tokens, create a new file, name it what you'd like and paste the following code in it. (Kotlin example shown below)

```kotlin
import android.annotation.SuppressLint
import com.courier.android.notifications.presentNotification
import com.courier.android.service.CourierService
import com.google.firebase.messaging.RemoteMessage

// This is safe. `CourierService` will automatically handle token refreshes.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class YourNotificationService: CourierService() {

    override fun showNotification(message: RemoteMessage) {
        super.showNotification(message)

        // TODO: This is where you will customize the notification that is shown to your users
        // `message.presentNotification(...)` is used to get started quickly and not for production use.
        // More information on how to customize an Android notification here:
        // https://developer.android.com/develop/ui/views/notifications/build-notification

        message.presentNotification(
            context = this,
            handlingClass = MainActivity::class.java,
            icon = android.R.drawable.ic_dialog_info
        )

    }

}
```

Next, add the `CourierService` entry in your `AndroidManifest.xml` file

```xml
<manifest>
    <application>

        <activity>
            ..
        </activity>

        // Add this 👇
        <service
            android:name=".YourNotificationService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        // Add this 👆

        ..

    </application>
</manifest>
```

Finally, add the `CourierActivity`. This will allow you to handle when users get push notifications delivered and when they click on the push notifications. You will likely want to extend your `MainActivity` but your use case may be slightly different.
    
```kotlin
class MainActivity : CourierActivity() {

    ..

    override fun onPushNotificationClicked(message: RemoteMessage) {
        Toast.makeText(this, "Message clicked:\n${message.data}", Toast.LENGTH_LONG).show()
    }

    override fun onPushNotificationDelivered(message: RemoteMessage) {
        Toast.makeText(this, "Message delivered:\n${message.data}", Toast.LENGTH_LONG).show()
    }

}
```

### Manual Token Syncing

If you do not want to use `CourierService` and `CourierActivity`, you can manually sync push notification tokens with the following code.

```kotlin
lifecycleScope.launch {
            
    val fcm = CourierPushProvider.FIREBASE_FCM

    // Save a token into Courier
    // If your user is signed in, this will succeed.
    // If your user is not signed in, this will be stored locally
    // and uploaded when you sign your user in.
    Courier.shared.setToken(
        provider = fcm,
        token = "your_messaging_token",
    )

    // Get a token
    // Looks up the locally stored token
    val fcmToken = Courier.shared.getToken(
        provider = fcm
    )
    
    print(fcmToken)
    
}
```

### Manual Notification Tracking

This is how you can tell Courier when a notification has been delivered or clicked in your app. If you are using `CourierService` and `CourierActivity`, this is done automatically.

```kotlin
lifecycleScope.launch {

    val client = Courier.shared.client // or use CourierClient(...)

    client.tracking.postTrackingUrl(
        url = "https://courier.com/your_tracking_url",
        event = CourierTrackingEvent.CLICKED,
    )

}
```

&emsp;

## 4. Authenticate User

This will take the tokens you are syncing above and upload them to the `userId` you provide here.

See [`Authentication`](https://github.com/trycourier/courier-android/blob/master/Docs/Authentication.md) for more details.

```kotlin
lifecycleScope.launch {

    Courier.shared.signIn(
        accessToken = "pk_prod_H12...",
        clientKey = "YWQxN...",
        userId = "example_user_id"
    )

}
```

## 5. Register for Notifications

This is only needed for Android `33` and above. You are safe to call it in older versions of Android.

```kotlin
// Shows the request notification popup
Courier.shared.requestNotificationPermission(activity)

// Gets the value of the permission
val isGranted = Courier.shared.isPushPermissionGranted(context)
```

## 6. Sending a message

<table>
    <thead>
        <tr>
            <th width="600px" align="left">Provider</th>
            <th width="200px" align="center">Link</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels/firebase-fcm">
                    <code>(FCM) - Firebase Cloud Messaging</code>
                </a>
            </td>
            <td align="center">
                <a href="https://www.courier.com/docs/platform/channels/push/firebase-fcm/#sending-messages">
                    <code>Testing Docs</code>
                </a>
            </td>
        </tr>
    </tbody>
</table>

---

👋 `TokenManagement APIs` can be found <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Client.md#token-management-apis"><code>here</code></a>
