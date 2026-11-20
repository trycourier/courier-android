<!-- Generated from https://www.courier.com/docs/sdk-libraries/android by mintlify-docs. Edit the docs source, not this file. -->

# Courier Android SDK

> Prebuilt inbox and preferences UI, push token sync, and Kotlin APIs for Android. Install from Jitpack, add Firebase, and render Compose or XML views.

Source: https://www.courier.com/docs/sdk-libraries/android

The Courier Android SDK gives you prebuilt UI components and Kotlin APIs. It handles authentication, token management, and real-time message delivery.

This page is the Android reference: what to install, how to configure Gradle, and the Kotlin API surface. Each flow is written once and applies to every platform.

| To do this | Read |
| :-- | :-- |
| Render the inbox | [Add an inbox](https://www.courier.com/docs/in-app/add-an-inbox?lang=Android) |
| Theme it, handle taps, or build your own UI | [Customize the inbox](https://www.courier.com/docs/in-app/customize-the-inbox?lang=Android) |
| Set up push, from provider to device | [Set up push notifications](https://www.courier.com/docs/guides/set-up-mobile-push) |
| Add the preference center | [Add a preference center](https://www.courier.com/docs/in-app/add-a-preference-center?lang=Android) |
| Mint the JWT you sign in with | [Authenticate users](https://www.courier.com/docs/in-app/authenticate-users?lang=Android) |

Available on
[GitHub](https://github.com/trycourier/courier-android).

| Requirement | Value |
|-------------|-------|
| Min Android SDK | 23 |
| Compile SDK | 34 or later. The SDK is built and tested against API 36, and your app can target 36. |
| Java | 11 or later |
| Kotlin | 1.8 or later |
| Gradle | 8.4+ |
| Package manager | Jitpack |

## Installation

### Add the Jitpack repository

In your `settings.gradle` or `settings.gradle.kts`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Add the dependency

In your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.trycourier:courier-android:LATEST_VERSION'
}
```

Replace `LATEST_VERSION` with the current version from [GitHub Releases](https://github.com/trycourier/courier-android/releases).

### Initialize the SDK

Call `Courier.initialize()` in your `Application` class before using other SDK features. This gives Courier access to `SharedPreferences` to persist state across sessions.

```kotlin
class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Courier.initialize(this)
    }
}
```

### Use a Material theme

The prebuilt inbox and preference center only render correctly when your app theme extends `Theme.MaterialComponents`. Set it in `res/values/themes.xml`.

```xml
<style name="Theme.YourApp" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <!-- your attributes -->
</style>
```

> **Note:** If you only use [`CourierClient`](https://www.courier.com/docs/sdk-libraries/android#courierclient) APIs directly, skip the `initialize` and theme steps.

## Authentication

Courier authenticates with a **JWT** that your backend mints with your Courier API key, never in client code. [Authenticate users](https://www.courier.com/docs/in-app/authenticate-users?lang=Android) covers the token flow, scope strings, reading auth state, signing out, and token refresh. Using the EU datacenter? See [EU setup](https://www.courier.com/docs/in-app/authenticate-users?lang=Android#connect-to-the-eu-datacenter).

Credentials persist across app sessions, so you sign in once per user rather than on every launch. From 6.1.3, a relaunched app restores the signed-in user and uploads the device's push token again. From 6.1.4, the access token and client key are stored encrypted with a key held in the Android Keystore. Signing out deletes the device's push tokens from Courier.

```kotlin
lifecycleScope.launch {
  Courier.shared.signIn(accessToken = jwt, userId = "your_user_id")
  Courier.shared.signOut()
}
```

## Inbox

[Add an inbox](https://www.courier.com/docs/in-app/add-an-inbox?lang=Android) is the setup flow. These are the types it uses on Android.

| Type | What it is |
| :-- | :-- |
| `CourierInbox()` | The prebuilt inbox as a Jetpack Compose composable |
| `com.courier.android.ui.inbox.CourierInbox` | The same inbox as an XML layout view |
| `CourierInboxTheme` | Fonts, colors, unread indicators, swipe actions, button styles |
| `addInboxListener` | Raw message data and events, for a UI you render yourself |

Every callback and action these expose is documented per platform in [Data and actions](https://www.courier.com/docs/in-app/build-a-custom-inbox?lang=Android#data-and-actions).

### Inbox theme

`CourierInboxTheme` is a constructor argument on the composable, and a property on the view. Light and dark themes are separate.

```kotlin
CourierInbox(
    modifier = Modifier.padding(innerPadding),
    canSwipePages = true,
    lightTheme = yourLightTheme,
    darkTheme = yourDarkTheme,
    ...
)
```

Branding from [Courier Studio](https://app.courier.com/content/brands) also applies. The SDK reads your brand's primary color.

## Push notifications

[Set up push notifications](https://www.courier.com/docs/guides/set-up-mobile-push) is the setup flow, from connecting FCM to a device that receives push. It covers the `FirebaseMessagingService` and `CourierActivity` wiring, `setToken`, permission, and reading the payload on tap.

> **Note:** **Firebase is a separate dependency.** From Courier Android 6.x the SDK no longer bundles Firebase Messaging. Declare your own Firebase BoM and `firebase-messaging` artifact so you can subclass `FirebaseMessagingService`.

```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:33.1.2')
    implementation "com.google.firebase:firebase-messaging"
}
```

> **Note:** Push notifications require a physical device and a release build for reliable token registration and delivery. Emulators are best-effort.

Two entry points are Android-only, and neither has a counterpart on the other platforms.

| Call | When |
| :-- | :-- |
| `Courier.shared.refreshFcmToken()` | Force Firebase to hand the SDK a fresh token |
| `Courier.shared.isPushPermissionGranted(context)` | Read permission without prompting |

## Preferences

[Add a preference center](https://www.courier.com/docs/in-app/add-a-preference-center?lang=Android) is the setup flow. These are the types it uses on Android.

| Type | What it is |
| :-- | :-- |
| `CourierPreferences()` | The prebuilt preference center as a composable |
| `com.courier.android.ui.preferences.CourierPreferences` | The same view for XML layouts |
| `mode` | `Mode.Topic` or `Mode.Channels(listOf(PUSH, SMS, EMAIL))` |
| `CourierPreferencesTheme` | Fonts, colors, toggle styles, section headers |

> **Note:** The composable renders the default channels mode. Set `mode` on the XML view for topic mode.

> **Info:** Topics and sections are configured in the [Preferences Editor](https://www.courier.com/docs/guides/build-a-preference-center#configure-the-topics). See [Preferences Overview](https://www.courier.com/docs/recipients/preferences/overview) for how preference enforcement works at send time.

### Preferences theme

Light and dark themes are separate, and [Courier Studio branding](https://app.courier.com/content/brands) applies automatically when you supply a `brandId`.

## CourierClient

For advanced use cases, `CourierClient` is a low-level wrapper around the Courier API. Each client holds its own credentials, so you can spin up as many as you need.

### Initialization

```kotlin
val client = CourierClient(
    jwt          = "...",          // Optional. Required for most authenticated calls
    userId       = "your_user_id",
    connectionId = "...",          // Optional. Used for the inbox websocket
    tenantId     = "...",          // Optional. Scopes the client to a tenant
    apiUrls      = CourierClient.ApiUrls.eu(), // Optional. Use for EU users
    showLogs     = true            // Optional. Defaults to your build configuration
)

val options = client.options
client.log("...")
client.warn("...")
client.error("...")
```

### Token management

```kotlin
client.tokens.putUserToken(
    token = "...",
    provider = "firebase-fcm"
)

// Customize device metadata stored alongside the token
val device = CourierDevice(
    appId        = "APP_ID",
    adId         = "AD_ID",
    deviceId     = "DEVICE_ID",
    platform     = "android",
    manufacturer = "Google",
    model        = "Pixel 8"
)

// Or use what the SDK can detect about the current device
// CourierDevice.current(context)

client.tokens.putUserToken(
    token = "...",
    provider = "firebase-fcm",
    device = device
)

client.tokens.deleteUserToken(token = "...")
```

### Inbox

```kotlin
val messages = client.inbox.getMessages(
    paginationLimit = 25,
    startCursor = null
)

val archived = client.inbox.getArchivedMessages(
    paginationLimit = 25,
    startCursor = null
)

val unreadCount = client.inbox.getUnreadMessageCount()

client.inbox.open(messageId = "...")
client.inbox.read(messageId = "...")
client.inbox.unread(messageId = "...")
client.inbox.archive(messageId = "...")
client.inbox.readAll()
```

### Inbox websocket

Receive real-time updates across devices:

```kotlin
val socket = client.inbox.socket

socket.onOpen  = { print("Socket opened") }
socket.onClose = { code, reason -> print("Socket closed: $code, $reason") }
socket.onError = { error -> print(error) }

socket.receivedMessage = { message -> print(message) }
// Fires only when the event comes from a different connectionId
// Available events: READ, UNREAD, MARK_ALL_READ, OPENED, ARCHIVE
socket.receivedMessageEvent = { event -> print(event) }

lifecycleScope.launch {
    socket.connect()
    socket.sendSubscribe()

    // When done:
    socket.disconnect()
}
```

### Preferences

```kotlin
val prefs = client.preferences.getUserPreferences(paginationCursor = null)

val topic = client.preferences.getUserPreferenceTopic(topicId = "...")

client.preferences.putUserPreferenceTopic(
    topicId = "...",
    status = CourierPreferenceStatus.OPTED_IN,
    hasCustomRouting = true,
    customRouting = listOf(CourierPreferenceChannel.PUSH)
)
```

### Branding

```kotlin
val brand = client.brands.getBrand(brandId = "...")
```

### URL tracking

Pass any tracking URL found inside a push notification payload or inbox message:

```kotlin
// Available events: CLICKED, DELIVERED, OPENED, READ, UNREAD
client.tracking.postTrackingUrl(
    url = "courier_tracking_url",
    event = CourierTrackingEvent.DELIVERED
)
```

See the full [Courier API reference](https://www.courier.com/docs/reference/api-overview).
