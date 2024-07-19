# `CourierClient`

Base layer Courier API wrapper.

## Initialization

Creating a client stores request authentication credentials only for that specific client. You can create as many clients as you'd like. More info about authentication can be found <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Authentication.md#going-to-production"><code>here</code></a>

```kotlin
// Creating a client
val client = CourierClient(
    jwt          = "...",          // Optional. Likely needed for your use case. See above for more authentication details.
    clientKey    = "...",          // Optional. Used only for Inbox
    userId       = "your_user_id",
    connectionId = "...",          // Optional. Used for inbox websocket
    tenantId     = "...",          // Optional
    showLogs     = ..,             // Optional. Defaults to your current BuildConfig
)

// Details about the client
val options = client.options

// Logging to the console
client.log("...")
client.warn("...")
client.error("...")
```

## Token Management APIs

All available APIs for Token Management

```kotlin
// Saves a token into Courier Token Management
client.tokens.putUserToken(
    token = "...",
    provider = "firebase-fcm",
)

// To customize the device of the token being saved
val device = CourierDevice(
    app_id       = "APP_ID",    // Optional
    ad_id        = "AD_ID",     // Optional
    device_id    = "DEVICE_ID", // Optional
    platform     = "android",   // Optional
    manufacturer = "Google",    // Optional
    model        = "Pixel 123"  // Optional
)

// Alternatively, you can use CourierDevice.current to get what
// the Courier SDK can find about the current device being use
// CourierDevice.current is the default param

client.tokens.putUserToken(
    token = "...",
    provider = "firebase-fcm",
    device = device,
)

// Deletes the token from Courier Token Management
client.tokens.deleteUserToken(
    token = "...",
)
```

## Inbox APIs

All available APIs for Inbox

```kotlin
// Get all inbox messages
// Includes the total count in the response
val messages = client.inbox.getMessages(
    paginationLimit = 123, // Optional
    startCursor = null,    // Optional
)

// Returns only archived messages
// Includes the total count of archived message in the response
val archivedMessages = client.inbox.getArchivedMessages(
    paginationLimit = 123, // Optional
    startCursor = null,    // Optional
)

// Gets the number of unread messages
val unreadCount = client.inbox.getUnreadMessageCount()

// Tracking messages
client.inbox.trackOpened(messageId = "...")
client.inbox.trackRead(messageId = "...")
client.inbox.trackUnread(messageId = "...")
client.inbox.trackAllRead()

// Inbox Websocket
client.inbox.socket.apply {

    onOpen = {
        print("Socket Opened")
    }

    onClose = { code, reason ->
        print("Socket closed: $code, $reason")
    }

    onError = { error ->
        print(error)
    }

    // Returns the event received
    // Available events: READ("read"), UNREAD("unread"), MARK_ALL_READ("mark-all-read"), OPENED("opened"), ARCHIVE("archive")
    // Note: This will not fire unless you provide a connectionId to the client and the event comes from another app using a different connectionId
    receivedMessageEvent = { event ->
        print(event)
    }

    // Returns the InboxMessage received
    receivedMessage = { message ->
        print(message)
    }

    connect()       // Connects the socket
    sendSubscribe() // Subscribes to socket events for the user id in the client
    disconnect()    // Disconnects the socket

}
```

## Preferences APIs

All available APIs for Preferences

```kotlin
// Get all the available preference topics
client.preferences.getUserPreferences(
    paginationCursor = null, // Optional
)

// Gets a specific preference topic
client.preferences.getUserPreferenceTopic(
    topicId = "...",
)

// Updates a user preference topic
client.preferences.putUserPreferenceTopic(
    topicId = "...",
    status = CourierPreferenceStatus.OPTED_IN,
    hasCustomRouting = true,
    customRouting = listOf(CourierPreferenceChannel.PUSH),
)
```

## Branding APIs

All available APIs for Branding

```kotlin
client.brands.getBrand(
    brandId = "...",
)
```

## URL Tracking APIs

All available APIs for URL Tracking

```kotlin
// Pass a trackingUrl, usually found inside of a push notification payload or Inbox message
// Tell which event happened. All available events:
// CLICKED("CLICKED"), DELIVERED("DELIVERED"), OPENED("OPENED"), READ("READ"), UNREAD("UNREAD")
client.tracking.postTrackingUrl(
    url = "courier_tracking_url",
    event = CourierTrackingEvent.DELIVERED,
)
```

See the full suite of Courier APIs <a href="https://www.courier.com/docs/reference/"><code>here</code></a>
