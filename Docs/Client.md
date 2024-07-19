```kotlin
val client = CourierClient(
    jwt = "...",
    clientKey = "...",
    userId = "user_id",
    connectionId = "uuid_used_for_websockets", // Optional
    tenantId = "tenant_id", // Optional
    showLogs = true, // Optional. Defaults to BuildConfig
)

// Token Management

val device = CourierDevice(
    app_id = "APP_ID",
    ad_id = "AD_ID",
    device_id = "DEVICE_ID",
    platform = "android",
    manufacturer = "Google",
    model = "Pixel 99"
)

client.tokens.putUserToken(
    token = "...",
    provider = "firebase-fcm",
    device = device, // Optional
)

client.tokens.deleteUserToken(
    token = "...",
)

// Inbox

client.inbox.getMessages(
    paginationLimit = 123, // Optional
    startCursor = null, // Optional
)

client.inbox.getArchivedMessages(
    paginationLimit = 123, // Optional
    startCursor = null, // Optional
)

val count = client.inbox.getUnreadMessageCount()

client.inbox.trackOpened(
    messageId = "...",
)

client.inbox.trackRead(
    messageId = "...",
)

client.inbox.trackUnread(
    messageId = "...",
)

client.inbox.trackAllRead()

val socket = client.inbox.socket

socket.onOpen = {
    println("Socket Opened")
}

socket.onClose = { code, reason ->
    println("Socket closed: $code, $reason")
}

socket.onError = { error ->
    assertNull(error)
}

socket.receivedMessageEvent = { event ->
    println(event)
}

socket.receivedMessage = { message ->
    println(message)
    hold2 = false
}

socket.connect()
socket.sendSubscribe() // Needed to listen to socket events
socket.disconnect()

// Preferences

client.preferences.getUserPreferences(
    paginationCursor = null, // Optional
)

client.preferences.getUserPreferenceTopic(
    topicId = "...",
)

client.preferences.putUserPreferenceTopic(
    topicId = "...",
    status = CourierPreferenceStatus.OPTED_IN,
    hasCustomRouting = true,
    customRouting = listOf(CourierPreferenceChannel.PUSH),
)

// Branding

client.brands.getBrand(
    brandId = "...",
)

// Tracking

client.tracking.postTrackingUrl(
    url = "courier_tracking_url",
    event = CourierTrackingEvent.DELIVERED, // Clicked etc are supported
)
```
