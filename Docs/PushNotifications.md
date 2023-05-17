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
                <a href="TODO">
                    <code>Automatic Token Management</code>
                </a>
            </td>
            <td align="left">
                Skip manually managing push notification device tokens.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="TODO">
                    <code>Notification Tracking</code>
                </a>
            </td>
            <td align="left">
                Track if your users are receiving your notifications even if your app is not runnning or open.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="TODO">
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
                A phyical Android device
            </td>
            <td align="left">
                Although you can setup the Courier SDK without a device, a physical device is the only way to full ensure push notification tokens and notification delivery is working correctly. Simulators are not reliable.
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
    </tbody>
</table>

&emsp;

# Setup

## 1. Setup a Push Notification Provider

Select which push notification provider you would like Courier to route push notifications to. Choose (FCM) - Firebase Cloud Messaging if you are not sure which provider to use.

<table>
    <thead>
        <tr>
            <th width="600px" align="left">Provider</th>
            <th width="200px" align="center">Token Syncing</th>
            <th width="200px" align="center">Supported</th>
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
                <code>Automatic</code>
            </td>
            <td align="center">✅</td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/channels/onesignal">
                    <code>OneSignal</code>
                </a>
            </td>
            <td align="center">
                —
            </td>
            <td align="center">❌</td>
        </tr>
    </tbody>
</table>

&emsp;

## 2. Add the `CourierService`

1. Create the `CourierService` file

This file is used to track new push notifications when they arrive and to automatically sync push notification tokens. Create a new file, name it what you'd like and paste the follow code in it. (Kotlin example shown below)

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
        // The function below is used to get started quickly.
        // You likely do not want to use `message.presentNotification(...)`
        // For Flutter, you likely do not want to change the handlingClass
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

&emsp;

2. Add the `CourierService` entry in your `AndroidManifest.xml` file

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

&emsp;

## 3. Extend `CourierActivity`

This will allow you to handle when users get push notifications delivered and when they click on the push notifications. You will likely want to extend your `MainActivity` but your use case may be slightly different.
    
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

&emsp;

## 4. Send a Test Push Notification

```kotlin
lifecycleScope.launch {

    Courier.shared.signIn(
        accessToken = "pk_prod_H12...",
        clientKey = "YWQxN...",
        userId = "example_user_id"
    )
    
    val hasNotificationPermissions = (activity as AppCompatActivity).requestNotificationPermission()
    print(hasNotificationPermissions)

}
```

1. Register for push notifications


```kotlin
TODO
```

2. Send a test message

[`More Testing Examples Here`](https://github.com/trycourier/courier-android/blob/master/Docs/Testing.md)
