# **🐤 Courier — Android**

Courier helps you spend less time building notification infrastructure, and more time building great experiences for your users!

[https://courier.com](https://www.courier.com/)

⚠️ This SDK is in Beta and actively maintained

# Courier Android Overview

```kotlin
// Must be initialized before Courier is started
// because Courier Android depends on Firebase Cloud Messaging (FCM)
FirebaseApp.initializeApp(...)

// Initialize Courier
// Must be called before you can use the SDK
Courier.initialize(context)

// The user id you wish to register data with in Courier
// You likely want this to match your existing authentication system user ids
val userId = "asdfasdf"

// The key used to make requests to Courier
// THIS KEY SHOULD NOT LIVE IN YOUR PRODUCTION APP
// You should only use this key to test with
// https://app.courier.com/settings/api-keys
val authKey = "pk_prod_ABCD..."

// The access token used to make user specific changes to Courier data
// This token is safe to be used in your production app and should be generate by your backend
val accessToken = "eyJhbGciOiJIUzI..."

// Enables debug log for Courier
Courier.shared.isDebugging = true

// Set your Courier credentials
// These credentials will persist between app sessions
// If you close your app and open it again, your accessToken and userId will still be there
Courier.shared.signIn(
    accessToken = authKey or accessToken,
    userId = userId
)

// Get your fcmToken
val fcmToken= Courier.shared.fcmToken

// Send a message to your device
// This should only be used for testing purposes
// Calling this will send a push notification to all valid tokens
// For this userId and the providers you declare
val requestId = Courier.shared.sendPush(
    authKey = authKey,
    userId = userId,
    title = "This is a title",
    body = "This is a message",
    providers = listOf(CourierProvider.FCM)
)

// Sign the current Courier user out
// This will delete the current FCM token for the user in Courier
// Then it will remove all local user state for the current user
Courier.shared.signOut()

// Requests notification permission 
// Returns permission status
import com.courier.android.requestNotificationPermission
val requestPermissionStatus = requestNotificationPermission()

// Returns notification permission status
import com.courier.android.getNotificationPermissionStatus
val currentPermissionStatus = await Courier.shared.getNotificationPermissionStatus()

// Handling push notification interactions
import com.courier.android.activity.CourierActivity
class YourActivity : CourierActivity() {

    // Called when a push notification is tapped
    override fun onPushNotificationClicked(message: RemoteMessage) {
        print(message)
    }

    // Called when the notification is delivered to the device
    // Will only get called when app is in foreground or background state
    // Not when app is in "killed" or "not running" state
    override fun onPushNotificationDelivered(message: RemoteMessage) {
        print(message)
    }

}

// Handling push notification presentation and automatic token syncing
class YourMessagingService: CourierService() {

    // Called when you are safe to show your notification to the user
    override fun showNotification(message: RemoteMessage) {
        
        // A simple function for showing a push notification
        // You likely want this to be something custom for your app
        message.presentNotification(
            context = this,
            handlingClass = YourActivity::class.java,
            icon = R.drawable.your_drawable
        )

    }

}

```

# Requirements & Support

| Min SDK | Compile SDK | Kotlin | Java | Firebase Cloud Messaging | Expo | OneSignal | Courier Inbox | Courier Toast |
| :--: | :--: | :--: | :--: | :--: | :--: | :--: | :--: | :--: |
| `21` |     `33` |  ✅ |    ✅ |✅ |   ❌ |         ❌ |            ❌ |            ❌ |

> Most of this SDK depends on a Courier account: [`Create a Courier account here`](https://app.courier.com/signup)

> Testing push notifications requires a physical device. Simulators will not work.

Misc info about the SDK:
- All async functionality is executed on background threads
- The SDK does support runtime push notification permissions found in Android 13+ (API 33)
- The SDK automatically maintains your user's state between app sessions using `SharedPreferences`
- To best test the SDK, you should use a physical Android device
- All functions support Coroutines!

# **Installation**

>
> Link to [`Example App`](https://github.com/trycourier/courier-android)
>

1. [`Add dependency`](#1-install-the-package)
2. [`Setup`](#2-setup)
3. [`Configure Push Provider`](#3-configure-push-provider)
4. [`Managing User State`](#4-managing-user-state)
5. [`Testing Push Notifications`](#5-testing-push-notifications)

&emsp;

## **1. Add dependency**

#### 1. Add Jitpack repository support in your settings.gradle file
Courier Android is currently distributed via Jitpack. Maven Central support will be coming in a future update.
```gradle
pluginManagement {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
dependencyResolutionManagement {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

#### 2. Add the implementation to your app build.gradle file
``` gradle
dependencies {
    implementation 'com.github.trycourier:courier-android:1.1.0'
}
```
&emsp;

## **2. Setup**
1. Run Gradle sync
2. Change your `MainActivity` to extend the `CourierActivity`
    - This allows Courier to handle when push notifications are delivered and clicked
3. Setup a new Notification Service by creating a new file and pasting the code below in it
    - This allows you to present a notification to your user when a new notification arrives

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


4. Add the Notification Service entry in your `AndroidManifest.xml` file

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
#### **Important: Payload Data Override**

To ensure `CourierService.showNotification()` gets triggered for every possible state your app can be in (foreground, background & killed), you need to structure your `firebase-fcm` payload in the Courier Send endpoint like the following.

Courier is working on improving this area.

```JSON
{
  "message": {
    "to": {
      "user_id": "example_user"
    },
    "content": {
      "title": "Hi! 👋",
      "body": "Chrip Chirp!"
    },
    "routing": {
      "method": "all",
      "channels": [
        "firebase-fcm"
      ]
    },
    "providers": {
      "firebase-fcm": {
        "override": {
          "body": {
            "notification": null,
            "data": {
              "title": "Hi! 👋",
              "body": "Chrip Chirp!"
            }
          }
        }
      }
    }
  }
}
```

_[More about the Send API](https://www.courier.com/docs/reference/send/message/)_

If you do not override the `firebase-fcm` body your app will still receive the notification, but the notification will be handled by the Android system tray rather than the Service you implemented above.

This will not track the delivery of the notification properly and will not present the notification customizations you likely want to be applied.
&emsp;

## **3. Configure Push Provider**

> If you don't need push notification support, you can skip this step.

To get push notification to appear in your app, add support for the provider you would like to use:
- [`FCM (Firebase Cloud Messaging)`](https://www.courier.com/docs/guides/providers/push/firebase-fcm/)

You will need to get your Firebase Service Account key from this link:
```
https://console.firebase.google.com/project/${your_firebase_project_id}/settings/serviceaccounts/adminsdk
```
&emsp;

## **4. Managing User State**

Best user experience practice is to synchronize the current user's push notification tokens and the user's state. Courier does most of this for you automatically!

> You can use a Courier Auth Key [`found here`](https://app.courier.com/settings/api-keys) when developing.

> When you are ready for production release, you should be using a JWT as the `accessToken`.
> Here is more info about [`Going to Production`](#going-to-production)

Place these functions where you normally manage your user's state:
```kotlin
// Saves accessToken and userId to native level local storage
// This will persist between app sessions
Courier.shared.signIn(
    accessToken = authKey or accessToken,
    userId = userId
)

Courier.shared.signOut()
```

If you followed the steps above FCM tokens on Android will automatically be synced to Courier
&emsp;

## **5. Testing Push Notifications**

> If you don't need push notification support, you can skip this step.

Courier allows you to send a push notification directly from the SDK to a user id. No tokens juggling or backend needed!

```kotlin
import kotlinx.coroutines.launch
import com.courier.android.activity.CourierActivity
import com.courier.android.notifications.CourierPushNotificationCallbacks
import com.courier.android.requestNotificationPermission

class MainActivity : CourierActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Courier.initialize(context = this)
        
        lifecycleScope.launch {
            // Notification permissions must be true
            val hasNotificationPermissions = requestNotificationPermission();
            print(requestedNotificationPermission);
        }

    }

    // Will be called if the app is in the foreground and a push notification arrives
    override fun onPushNotificationClicked(message: RemoteMessage) {
        print(message)
    }

    // Will be called when a user clicks a push notification
    override fun onPushNotificationDelivered(message: RemoteMessage) {
        print(message)
    }
    
    fun sendTestPush() {
    
        lifecycleScope.launch {
            
            val messageId =  Courier.shared.sendPush(
                authKey: 'a_courier_auth_key_that_should_only_be_used_for_testing',
                userId: 'example_user',
                title: 'Chirp Chrip!',
                body: 'Hello from Courier 🐣',
                isProduction: false, // This only affects APNS pushes. false == sandbox / true == production
                providers: [CourierProvider.fcm],
            );
            
        }
        
    }
    
}
```

&emsp;

## **Going to Production**

For security reasons, you should not keep your `authKey` (which looks like: `pk_prod_ABCD...`) in your production app. The `authKey` is safe to test with, but you will want to use an `accessToken` in production.

To create an `accessToken`, call this: 

```curl
curl --request POST \
     --url https://api.courier.com/auth/issue-token \
     --header 'Accept: application/json' \
     --header 'Authorization: Bearer $YOUR_AUTH_KEY' \
     --header 'Content-Type: application/json' \
     --data
 '{
    "scope": "user_id:$YOUR_USER_ID write:user-tokens",
    "expires_in": "$YOUR_NUMBER days"
  }'
```

Or generate one here:
[`Issue Courier Access Token`](https://www.courier.com/docs/reference/auth/issue-token/)

> This request to issue a token should likely exist in a separate endpoint served on your backend.

&emsp;

## **Share feedback with Courier**

We want to make this the best SDK for managing notifications! Have an idea or feedback about our SDKs? Here are some links to contact us:

- [Courier Feedback](https://feedback.courier.com/)
- [Courier Android Issues](https://github.com/trycourier/courier-android/issues)

