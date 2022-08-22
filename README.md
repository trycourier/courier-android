# **🐤 Courier — Android**

Courier helps you spend less time building notification infrastructure, and more time building great experiences for your users!

[https://courier.com](https://www.courier.com/)

&emsp;

## **Quick Overview**

```kotlin
// Must be initialized before Courier is started
FirebaseApp.initializeApp(this, options)

val userId = "example_user"
val accessToken = "a_jwt_from_courier" // For more checkout -> https://www.courier.com/docs/reference/auth/issue-token/
val authKey = "your_auth_key_that_should_not_live_in_your_production_app"

// Set your Courier credentials
Courier.instance.setCredentials(
    accessToken = "your_access_token_and_not_your_api_key",
    userId = userId
)

// Send a message to your device
// This should only be used for testing purposes
Courier.sendPush(
    authKey = authKey,
    userId = userId,
    title = "This is a title",
    body = "This is a message"
)

// Sign the current user out
Courier.instance.signOut()
```
&emsp;

## **Installation (5 Steps)**

The following steps will get the Courier Android SDK setup and allow support for sending push notifications from Courier to your device. The following messaging providers are supported

The following messaging providers are supported:
- Firebase Cloud Messaging (FCM)

For a full example, clone the repo, install the dependencies, and run `app` found at the root directory.

⚠️ You may need a physical device to receive push notifications. You cannot test this effectively using the simulator.

This SDK supports kotlin coroutines for all major functions and has simple helpers for Android 13+ runtime push notification permissions.

&emsp;

### **1. Add the Gradle Dependency**

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

#### 2. Add the implementation to your build.gradle file
```
implementation 'com.github.trycourier:courier-android:1.0.0'
```

&emsp;

### **2. Manage User Credentials**

User Credentials must be set in Courier before they can receive push notifications. This should be handled where you normally manage your user's state.

⚠️ User Credentials should be [signed out](#5-signing-users-out) when you no longer want that user to receive push notifications.

⚠️ Courier does not maintain user state between app sessions, or in other words, if you force close the app, you will need to set user credentials again. We will be looking into maintaining user credential state between app sessions in future versions of this SDK.

```kotlin

fun signInWithCourier() {
    
    val userId = "example_user"
        
    // Courier needs you to generate an access token on your backend
    // Docs for setting this up: https://www.courier.com/docs/reference/auth/issue-token/
    val accessToken = "example_jwt"

    // Set Courier user credentials
    Courier.shared.setCredentials(
        accessToken = accessToken, 
        userId = userId, 
        onSuccess = {
            print("Credentials are set")
        }, 
        onFailure = { e ->
            print(e)
        }
    )
    
}
```

&emsp;

### **3. Add the Courier Service**

#### 1. Create a new class extending `CourierService`

This class will automatically manage firebase cloud messaging tokens and handle tracking Courier message delivery analytics.

```kotlin
class ExampleService: CourierService() {

    override fun showNotification(message: RemoteMessage) {
        super.showNotification(message)

        // This is how your notification will be presented to your user
        // You can use this function, but you likely want to customize this
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
<manifest ...>

    <application ...>

        <activity ...>

        <!-- Add this 👇 -->
        <service
            android:name=".ExampleService"
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

To ensure `CourierService.showNotification()` gets triggered for every possible state your app can be in, you need to structure your `firebase-fcm` payload in the Courier Send endpoint like the following.

_[More about the Send API](https://www.courier.com/docs/reference/send/message/)_

If you do not override the `firebase-fcm` body your app will still receive the notification, but the notification will be handled by the Android system tray rather than the Service you implemented above.

This will not track the delivery of the notification properly and will not present the notification customizations you likely want to be applied.


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

&emsp;

It's not recommened, but is possible to sync tokens and track messages manually with these functions.

```kotlin
// Set the token to the current user credentials
Courier.instance.setFCMToken(
    token = token,
    onSuccess = { Courier.log("Token set") },
    onFailure = { Courier.log(it.toString()) }
)

// Track a remote message payload
Courier.trackNotification(
    message = message,
    event = CourierPushEvent.DELIVERED,
    onSuccess = { Courier.log("Event tracked") },
    onFailure = { Courier.log(it.toString()) }
)
```

&emsp;

### **4. Handling Push Notifications**

The SDK has simple functions you can override to handle when you receive or click on a notification. Implement the following to handle this actions.

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

You can skip this step, but you will have to handle the above functions yourself.

&emsp;

### **5. Configure a Provider**

To get pushes to appear, add support for the provider you would like to use. Checkout the following tutorials to get a push provider setup.

- [Firebase Cloud Messaging](https://www.courier.com/docs/guides/providers/push/firebase-fcm/)

&emsp;

### **6. Signing Users Out**

Best user experience practice is to synchronize the current user's push notification tokens and the user's state. 

This should be called where you normally manage your user's state.

```kotlin
Courier.instance.signOut(
    onSuccess = {
        print("User signed out")
    },
    onFailure = { e ->
        print(e)
    }
)
```

&emsp;

### **Bonus! Sending a Test Push Notification**

⚠️ This is only for testing purposes and should not be in your production app.

```kotlin
Courier.sendPush(
    authKey = "your_api_key_that_should_not_stay_in_your_production_app",
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

### **Share feedback with Courier**

We want to make this the best SDK for managing notifications! Have an idea or feedback about our SDKs? Here are some links to contact us:

- [Courier Feedback](https://feedback.courier.com/)
- [Courier Android Issues](https://github.com/trycourier/courier-android/issues)
