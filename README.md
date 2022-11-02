# **🐤 Courier — Android**

Courier helps you spend less time building notification infrastructure, and more time building great experiences for your users!

[https://courier.com](https://www.courier.com/)

⚠️ This SDK is in Beta and actively maintained

&emsp;

## **SDK Overview**

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

// Set your Courier credentials
// These credentials will persist between app sessions
// If you close your app and open it again, your accessToken and userId will still be there
Courier.shared.signIn(
    accessToken = authKey or accessToken,
    userId = userId
)

// Send a message to your device
// This should only be used for testing purposes
// Calling this will send a push notification to all valid tokens
// for this userId and the providers you declare
Courier.shared.sendPush(
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

// Handling push notification interactions
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

Want to try a demo? Here is a link to a full sample project:
[Courier Android Sample App](https://github.com/trycourier/courier-android/tree/master/app)

Misc info about the SDK:
- All async functionality is executed on background threads
- The SDK does support runtime push notification permissions found in Android 13+ (API 33)
- The SDK automatically maintains your user's state between app sessions using `SharedPreferences`
- To best test the SDK, you should use a physical Android device
- All functions support Coroutines!

&emsp;

## **Full Installation (6 Steps)**

The following steps will get the Courier Android SDK setup and allow support for sending push notifications from Courier to your device.

&emsp;

### **1. Add the Dependency**

Courier Android is currently distributed via Jitpack. Maven Central support will be coming in a future update.

#### 1. Add Jitpack repository support in your settings.gradle file
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
    implementation 'com.github.trycourier:courier-android:1.0.28'

    // The firebase messaging dependency is also required
    implementation platform('com.google.firebase:firebase-bom:30.3.1')
    implementation 'com.google.firebase:firebase-messaging-ktx'
}
```

&emsp;

### **2.Sign in with Courier**

A user must be "signed in" with Courier before they can receive push notifications. This should be handled where you normally manage your user's state.

⚠️ Users should be [signed out](#6-signing-users-out) when you no longer want that user to receive push notifications on that device.

⚠️ Courier holds a local reference to the `accessToken` and `userId` you set using `Courier.shared.signIn(...)`. This allows your user to still be "signed in" between app sessions.

```kotlin
class YourApplication: Application() or YourActivity: AppCompatActivity() {
    
    ...
    
    fun initializeSDKs() {

        // Firebase must be initialized before Courier to receive messages via FCM (Firebase Cloud Messaging)
        FirebaseApp.initializeApp(...)

        // Initialize Courier
        // Must be called before you can use the Courier SDK
        // You are safe to move this to another place in your 
        // project where you normally initialize other SDKs
        Courier.initialize(context = this)
        
    }
    
}

fun signInWithCourier() {
    
    val userId = "your_user_id"
    
    // This key should only be used for testing and not be saved in your production app
    // Go to step 8 for more information about this
    val authKey = "pk_prod_ABCD..."

    // Set Courier user credentials
    Courier.shared.signIn(
        accessToken = authKey, 
        userId = userId, 
        onSuccess = {
            print("Courier user signed in")
        }, 
        onFailure = { e ->
            print(e)
        }
    )
    
}
```

⚠️ `authKey`s are safe to test with, but not safe to leave in you production app. When you are ready for production, make sure you complete this step: [Generate Production Access Token](#8-getting-production-ready)

&emsp;

### **3. Add the Courier Service**

#### 1. Create a new class extending `CourierService`

This class will automatically manage Firebase Cloud Messaging (FCM) tokens and handle tracking Courier message delivery analytics.

```kotlin
class YourMessagingService: CourierService() {

    override fun showNotification(message: RemoteMessage) {
        super.showNotification(message)

        // A simple function for showing a push notification
        // You likely want this to be something custom for your app
        // Full Android docs: https://developer.android.com/develop/ui/views/notifications/build-notification
        message.presentNotification(
            context = this,
            handlingClass = MainActivity::class.java,
            icon = android.R.drawable.ic_dialog_info
        )

    }

}
```

#### 2. Add the new service class to your `AndroidManifest`

```xml
<manifest ... >

    <application ... >

        <activity ... />

        <!-- Add this 👇 -->
        <service
            android:name=".YourMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        <!-- Add this 👆 -->

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

It's not recommended, but is possible to sync tokens and track notification delivery manually with the following functions.

```kotlin
// Set the token to the current user credentials
Courier.shared.setFCMToken(
    token = token,
    onSuccess = { Courier.log("Token set") },
    onFailure = { Courier.log(it.toString()) }
)

// Track a remote message payload
Courier.shared.trackNotification(
    message = message,
    event = CourierPushEvent.DELIVERED,
    onSuccess = { Courier.log("Event tracked") },
    onFailure = { Courier.log(it.toString()) }
)
```

`CourierActivity` and `CourierService` handle this functionality for you automatically.

&emsp;

### **4. Handling Push Notifications**

The SDK has simple functions you can override to handle when a user receives or taps on a notification.

If you skip this step you will have to handle the following functionality yourself.

```kotlin
class YourActivity : CourierActivity() {

    override fun onPushNotificationClicked(message: RemoteMessage) {
        print(message)
    }

    override fun onPushNotificationDelivered(message: RemoteMessage) {
        print(message)
    }

}
```

&emsp;

### **5. Configure a Provider**

To get pushes to appear, add support for the provider you would like to use. Here are links to get your providers configured:
- [Firebase Cloud Messaging](https://www.courier.com/docs/guides/providers/push/firebase-fcm/)

You will need to get your Firebase Service Account key from this link:
```
https://console.firebase.google.com/project/${your_firebase_project_id}/settings/serviceaccounts/adminsdk
```

&emsp;

### **6. Signing Users Out**

Best user experience practice is to synchronize the current user's push notification tokens with the user's state. This should be called where you normally manage your user's state.

When calling this function, Courier will delete the device token associated with the the current user, and then remove the locally stored user credentials.

```kotlin
Courier.shared.signOut(
    onSuccess = {
        print("User signed out")
    },
    onFailure = { e ->
        print(e)
    }
)
```

&emsp;

### **7. Sending a Test Push Notification**

⚠️ This is only for testing purposes and should not be in your production app.

```kotlin
Courier.shared.sendPush(
    authKey = authKey, // DO NOT LEAVE THE AUTH KEY IN YOUR PRODUCTION APP
    userId = "example_user",
    title = "Test message!",
    body = "Chrip Chirp!",
    providers = listOf(CourierProvider.FCM),
    onSuccess = { requestId ->
        print(requestId)
    },
    onFailure = { e ->
        print(e)
    }
)
```

&emsp;

### **8. Getting Production Ready**

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
[Issue Courier Access Token](https://www.courier.com/docs/reference/auth/issue-token/)

This request to issue a token should likely exist in a separate endpoint served on your backend.

&emsp;

### **Share feedback with Courier**

We want to make this the best SDK for managing notifications! Have an idea or feedback about our SDKs? Here are some links to contact us:

- [Courier Feedback](https://feedback.courier.com/)
- [Courier Android Issues](https://github.com/trycourier/courier-android/issues)
