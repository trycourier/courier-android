<!-- Generated from https://www.courier.com/docs/guides/set-up-mobile-push by mintlify-docs. Edit the docs source, not this file. -->

# Set up mobile push

> Connect FCM and APNs, sync device tokens with the mobile SDK on iOS, Android, React Native, or Flutter, send a test push, and track delivery and clicks.

Source: https://www.courier.com/docs/guides/set-up-mobile-push

A push needs three things: a provider connected in Courier, a device token on the user, and a send.

The Courier mobile SDK handles the token.

## Prerequisites

- [A Courier account](https://app.courier.com/) and an [API key](https://app.courier.com/settings/api-keys)
- Provider credentials: a Firebase service account JSON for Android, an APNs `.p8` key for iOS, or both
- A physical device to test on. On a Mac with Apple silicon, the iOS Simulator also receives push.

## Set up your app

### Connect FCM in Courier

In your [Firebase project settings](https://console.firebase.google.com/), open **Service Accounts** and click **Generate new private key**.

Then open [Integrations](https://app.courier.com/integrations), switch to the environment you send from, and choose **Firebase Cloud Messaging**. Paste the contents of the downloaded JSON into **Service Account JSON** and click **Install Provider**. Providers are per environment, so one connected in test doesn't exist in production.

Leave **Apply Recommended Courier Mobile SDK Formatting** on. Full walkthrough: [FCM integration](https://www.courier.com/docs/integrations/push/firebase-fcm).

### Install the SDK

Add the Jitpack repository in `settings.gradle`, then the dependency in your app's `build.gradle`. The SDK needs Android SDK 23 or later and Gradle 8.4 or later.

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```gradle
dependencies {
    implementation 'com.github.trycourier:courier-android:LATEST_VERSION'
}
```

Take `LATEST_VERSION` from [GitHub Releases](https://github.com/trycourier/courier-android/releases). Then initialize the SDK in your `Application` class so it can persist state across sessions.

```kotlin
class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Courier.initialize(this)
    }
}
```

### Add Firebase to your app

From Courier Android 6.x the SDK no longer bundles Firebase Messaging, so declare it yourself.

```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:33.1.2')
    implementation "com.google.firebase:firebase-messaging"
}
```

Then complete the [Firebase Android setup](https://firebase.google.com/docs/android/setup): download `google-services.json` into your app module and apply the Google Services plugin.

### Sync the device token

Subclass `FirebaseMessagingService` and forward both callbacks to Courier. `onNewToken` is what registers and refreshes the token.

```kotlin
package your.app.package

import com.courier.android.Courier
import com.courier.android.notifications.CourierPushNotificationIntent
import com.courier.android.notifications.presentNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CourierPushNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notificationIntent = CourierPushNotificationIntent(
            context = this,
            target = MainActivity::class.java,
            payload = message
        )

        notificationIntent.presentNotification(
            title = message.data["title"] ?: message.notification?.title,
            body = message.data["body"] ?: message.notification?.body
        )

        // Call this after presenting the notification: onMessageReceived makes a
        // synchronous tracking request that would otherwise delay the notification.
        Courier.onMessageReceived(message.data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Courier.onNewToken(token)
    }
}
```

Register the service in `AndroidManifest.xml`.

```xml
<service
    android:name=".CourierPushNotificationService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Then extend `CourierActivity` in your `MainActivity` to receive delivery and click events in the foreground.

```kotlin
class MainActivity : CourierActivity() {
    override fun onPushNotificationDelivered(pushNotification: Map<String, String>) { }
    override fun onPushNotificationClicked(pushNotification: Map<String, String>) { }
}
```

### Sign in the user

Courier ties every token to a signed-in user, so no push reaches the device until you call `signIn`. Pass the `user_id` you send to, plus a JWT your backend mints (see [Authenticate users](https://www.courier.com/docs/in-app/authenticate-users)).

> **Note:** **The `user_id` is yours, not Courier's.**
> Use whatever your app already calls this user, such as a database id or your auth provider's `sub`. Nothing has to exist in Courier first, since signing in registers the user and their device tokens. With no auth yet, any stable string works, as long as you reuse it on every launch and send to that same id.

```kotlin
// From an Activity or Fragment, where lifecycleScope is available
lifecycleScope.launch {
    Courier.shared.signIn(userId = "user_123", accessToken = jwt)
}
```

Call it once, right after your own login resolves. Credentials persist on the device, so app launches don't need it. Call `signOut` on logout to delete that device's tokens from Courier.

### Request notification permission

Android 13 and later (API 33) needs a runtime permission. Declare it in `AndroidManifest.xml`, then request it. The call is safe on older versions.

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

```kotlin
Courier.shared.requestNotificationPermission(activity)
```

Reading the status, and getting the user back after they deny: [Notification permissions](https://www.courier.com/docs/send/push/overview#notification-permissions).

Next: [send a test push](https://www.courier.com/docs/guides/set-up-mobile-push#send-a-test-push) to the `user_id` you signed in.

> **Info:** The SDK holds a token it receives before anyone signs in and uploads it at sign-in, so you never race the callback. Signing in a different user signs the previous one out first.

> **Tip:** To register tokens from a server or outside the SDK, use the [Device Tokens API](https://www.courier.com/docs/api-reference/device-tokens/add-a-token-to-a-user) directly. The [push notifications overview](https://www.courier.com/docs/integrations/push/overview) covers the token model.

## Send a test push

First [design a template](https://www.courier.com/docs/design/templates/overview) with a **Push** channel and publish it. Then send it to the `user_id` you signed in, routing to your push providers by their provider keys, `apn` and `firebase-fcm`. Courier resolves the user's tokens for each provider.

**Node.js**

```javascript
import Courier from '@trycourier/courier';

const client = new Courier({
  apiKey: process.env['COURIER_API_KEY'],
});

const { requestId } = await client.send.message({
  message: {
    to: { user_id: 'user_123' },
    template: 'nt_01kx4h2jdafq8bk9aftxak4b40',
    routing: { method: 'single', channels: ['apn', 'firebase-fcm'] },
  },
});

console.log(requestId);
```

**Python**

```python
import os
from courier import Courier

client = Courier(
    api_key=os.environ.get("COURIER_API_KEY"),
)
response = client.send.message(
    message={
        "to": {"user_id": "user_123"},
        "template": "nt_01kx4h2jdafq8bk9aftxak4b40",
        "routing": {"method": "single", "channels": ["apn", "firebase-fcm"]},
    },
)
print(response.request_id)
```

**cURL**

```bash
curl -X POST https://api.courier.com/send \
  -H "Authorization: Bearer $COURIER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "to": { "user_id": "user_123" },
      "template": "nt_01kx4h2jdafq8bk9aftxak4b40",
      "routing": { "method": "single", "channels": ["apn", "firebase-fcm"] }
    }
  }'
```

**Ruby**

```ruby
require "courier"

courier = Courier::Client.new(api_key: ENV["COURIER_API_KEY"])

response = courier.send_.message(
  message: {
    to: { user_id: "user_123" },
    template: "nt_01kx4h2jdafq8bk9aftxak4b40",
    routing: { method: "single", channels: ["apn", "firebase-fcm"] }
  }
)

puts(response)
```

**Go**

```go
response, err := client.Send.Message(context.TODO(), courier.SendMessageParams{
	Message: courier.SendMessageParamsMessage{
		To: courier.SendMessageParamsMessageToUnion{
			OfUserRecipient: &shared.UserRecipientParam{UserID: courier.String("user_123")},
		},
		Template: courier.String("nt_01kx4h2jdafq8bk9aftxak4b40"),
		Routing: courier.SendMessageParamsMessageRouting{
			Method:   string(shared.MessageRoutingMethodSingle),
			Channels: []shared.MessageRoutingChannelUnionParam{
				{OfString: courier.String("apn")},
				{OfString: courier.String("firebase-fcm")},
			},
		},
	},
})
```

**Java**

```java
SendMessageParams params = SendMessageParams.builder()
    .message(SendMessageParams.Message.builder()
        .to(JsonValue.from(java.util.Map.of("user_id", "user_123")))
        .template("nt_01kx4h2jdafq8bk9aftxak4b40")
        .routing(JsonValue.from(java.util.Map.of(
            "method", "single",
            "channels", java.util.List.of("apn", "firebase-fcm"))))
        .build())
    .build();
client.send().message(params);
```

**PHP**

```php
$response = $client->send->message(
  message: [
    'to' => ['userID' => 'user_123'],
    'template' => 'nt_01kx4h2jdafq8bk9aftxak4b40',
    'routing' => ['method' => 'single', 'channels' => ['apn', 'firebase-fcm']],
  ],
);
```

**C#**

```csharp
SendMessageParams parameters = new()
{
    Message = new()
    {
        To = new UserRecipient { UserID = "user_123" },
        Template = "nt_01kx4h2jdafq8bk9aftxak4b40",
        Routing = new Routing { Method = Method.Single, Channels = ["apn", "firebase-fcm"] },
    },
};

await client.Send.Message(parameters);
```

**CLI**

```bash
courier send message \
  --api-key "$COURIER_API_KEY" \
  --message '{"to":{"user_id":"user_123"},"template":"nt_01kx4h2jdafq8bk9aftxak4b40","routing":{"method":"single","channels":["apn","firebase-fcm"]}}'
```

**MCP**

```text
With Courier MCP, send my nt_01kx4h2jdafq8bk9aftxak4b40 template to user_123 through APNs, falling back to FCM.
```

## Verify

The notification appears on the device you registered. For a silent payload, confirm your app handled it in the background instead. Open the message in [Logs](https://app.courier.com/logs). A delivered push shows the provider it went through and the token it targeted. If it didn't deliver, the log names the reason: no token, expired token, or provider not configured.

> **Note:** `channels: ["apn", "firebase-fcm"]` tries APNs, then FCM, and stops at the first success. To reach both iOS and Android on one send, route to `push` and set `channels.push` to `{ "routing_method": "all", "providers": ["apn", "firebase-fcm"] }`. Naming the providers keeps Courier Inbox, which is also a push provider, out of the send. See [reach every device on push](https://www.courier.com/docs/send/routing#reach-every-device-on-push).

## Delivery and click tracking

Courier tracks push delivery and clicks for you. Every push carries a `trackingUrl`, and the SDK posts back to it as the notification arrives and again when the user taps it. Both events land in [Logs](https://app.courier.com/logs) with no code of your own.

The wiring from [Set up your app](https://www.courier.com/docs/guides/set-up-mobile-push#set-up-your-app) is what reports them:

| Event | Reported by | Works when |
| :-- | :-- | :-- |
| Delivered (iOS) | `CourierDelegate` | your app is in the foreground |
| Delivered (iOS) | the Notification Service Extension | your app is backgrounded or closed |
| Delivered (Android) | `FirebaseMessagingService` | any state, including killed |
| Clicked | `CourierDelegate`, `CourierActivity` | any state |

Two provider defaults keep that accurate, and a new workspace has both on. **Attach Mutable Content** on [APNs](https://www.courier.com/docs/integrations/push/apple-push-notification) is what lets the extension run at all. **Apply Recommended Courier Mobile SDK Formatting** on [FCM](https://www.courier.com/docs/integrations/push/firebase-fcm) ships the push as `data`, so Android wakes for it. [How the data arrives per provider](https://www.courier.com/docs/send/push/custom-data#how-the-data-arrives-per-provider) shows both payloads.

> **Note:** If you handle notifications outside the Courier hooks, post delivery and click events yourself with `client.tracking.postTrackingUrl`. Each SDK page carries the call: [iOS](https://www.courier.com/docs/sdk-libraries/ios#url-tracking), [Android](https://www.courier.com/docs/sdk-libraries/android#url-tracking), [Flutter](https://www.courier.com/docs/sdk-libraries/flutter#url-tracking), and [React Native](https://www.courier.com/docs/sdk-libraries/react-native#url-tracking).
