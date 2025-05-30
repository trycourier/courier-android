<img width="1000" alt="android-inbox-banner" src="https://github.com/trycourier/courier-android/assets/6370613/ccbe19de-5b08-4778-8f1f-c0c16f1c26e2">

&emsp;

&emsp;

# Courier Inbox

An in-app notification center list you can use to notify your users. Allows you to build high quality, flexible notification feeds very quickly.

## Requirements

<table>
    <thead>
        <tr>
            <th width="250px" align="left">Requirement</th>
            <th width="750px" align="left">Reason</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <tr width="600px">
                <td align="left">
                    <a href="https://app.courier.com/channels/courier">
                        <code>Courier Inbox Provider</code>
                    </a>
                </td>
                <td align="left">
                    Needed to link your Courier Inbox to the SDK
                </td>
            </tr>
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Authentication.md">
                    <code>Authentication</code>
                </a>
            </td>
            <td align="left">
                Needed to view inbox messages that belong to a user.
            </td>
        </tr>
    </tbody>
</table>

## Authentication

If you are using JWT authentication, be sure to enable JWT support on the Courier Inbox Provider [`here`](https://app.courier.com/integrations/catalog/courier).

<img width="385" alt="Screenshot 2024-12-09 at 11 19 31 AM" src="https://github.com/user-attachments/assets/71c945f3-9fa0-4736-ae0d-a4760cb49220">

## Theme

Your app theme must use the Material Components parent. This is for Material buttons. Set your `themes.xml` like this.

```xml
<resources>
    <style name="Your.Theme" parent="Theme.MaterialComponents.DayNight.DarkActionBar" />
</resources>
```

## Usage

`CourierInbox` works with all native Android UI frameworks.

<table>
    <thead>
        <tr>
            <th width="850px" align="left">UI Framework</th>
            <th width="200px" align="center">Support</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left"><code>XML</code></td>
            <td align="center">✅</td>
        </tr>
        <tr width="600px">
            <td align="left"><code>Programmatic</code></td>
            <td align="center">✅</td>
        </tr>
        <tr width="600px">
            <td align="left"><code>Jetpack Compose</code></td>
            <td align="center">✅</td>
        </tr>
    </tbody>
</table>

&emsp;

## Default Inbox Example

The default `CourierInbox` styles. Colors are using `colorPrimary` located in your `res/values/themes.xml` file.

<img width="441" alt="android-default-inbox-styles" src="https://github.com/user-attachments/assets/4e9779b9-9d0c-4408-ba12-917eceee7098">

&emsp;

### Jetpack Compose

```kotlin
CourierInbox(
    modifier = Modifier.padding(innerPadding),
    onClickMessageListener = { message, index ->
        if (message.isRead) message.markAsUnread() else message.markAsRead()
    },
    onLongPressMessageListener = { message, index ->
        message.markAsArchived()
    },
    onClickActionListener = { action, message, index ->
        print(message.toString())
    },
    onScrollInboxListener = { offsetInDp ->
        print(offsetInDp.toString())
    }
)
```

### Android Layouts

```xml
<com.courier.android.ui.inbox.CourierInbox xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/courierInbox"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val inbox: CourierInbox = view.findViewById(R.id.courierInbox)

inbox.setOnClickMessageListener { message, index ->
    Courier.log(message.toString())
    if (message.isRead) message.markAsUnread() else message.markAsRead()
}

inbox.setOnLongPressMessageListener { message, index ->
    Courier.log(message.toString())
}

inbox.setOnClickActionListener { action, message, index ->
    Courier.log(action.toString())
}

inbox.setOnScrollInboxListener { offsetInDp ->
    Courier.log(offsetInDp.toString())
}
```

&emsp;

## Styled Inbox Example

The styles you can use to quickly customize the `CourierInbox`.

<img width="441" alt="Screenshot 2024-12-09 at 11 19 31 AM" src="https://github.com/user-attachments/assets/a203fed8-4f7f-4e63-8f86-887c67338dad">

&emsp;

```kotlin
fun getTheme(context: Context): CourierInboxTheme {

    val whiteColor = Color(0xFFFFFFFF).toArgb()
    val blackColor = Color(0xFF000000).toArgb()
    val blackLightColor = Color(0x80000000).toArgb()
    val primaryColor = Color(0xFF6650a4).toArgb()
    val primaryLightColor = Color(0xFF625b71).toArgb()
    val font = ResourcesCompat.getFont(context, R.font.poppins)

    return CourierInboxTheme(
        loadingIndicatorColor = primaryColor,
        tabIndicatorColor = primaryColor,
        tabStyle = CourierStyles.Inbox.TabStyle(
            selected = CourierStyles.Inbox.TabItemStyle(
                font = CourierStyles.Font(
                    typeface = font,
                    sizeInSp = 18,
                    color = primaryColor
                ),
                indicator = CourierStyles.Inbox.TabIndicatorStyle(
                    font = CourierStyles.Font(
                        typeface = font,
                        sizeInSp = 14,
                        color = whiteColor
                    ),
                    color = primaryColor
                )
            ),
            unselected = CourierStyles.Inbox.TabItemStyle(
                font = CourierStyles.Font(
                    typeface = font,
                    sizeInSp = 18,
                    color = blackLightColor,
                ),
                indicator = CourierStyles.Inbox.TabIndicatorStyle(
                    font = CourierStyles.Font(
                        typeface = font,
                        sizeInSp = 14,
                        color = whiteColor
                    ),
                    color = blackLightColor,
                )
            )
        ),
        readingSwipeActionStyle = CourierStyles.Inbox.ReadingSwipeActionStyle(
            read = CourierStyles.Inbox.SwipeActionStyle(
                color = primaryColor
            ),
            unread = CourierStyles.Inbox.SwipeActionStyle(
                color = primaryLightColor
            )
        ),
        archivingSwipeActionStyle = CourierStyles.Inbox.ArchivingSwipeActionStyle(
            CourierStyles.Inbox.SwipeActionStyle(
                color = primaryColor
            )
        ),
        unreadIndicatorStyle = CourierStyles.Inbox.UnreadIndicatorStyle(
            indicator = CourierStyles.Inbox.UnreadIndicator.DOT,
            color = primaryColor
        ),
        titleStyle = CourierStyles.Inbox.TextStyle(
            unread = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 18
            ),
            read = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 18
            ),
        ),
        bodyStyle = CourierStyles.Inbox.TextStyle(
            unread = CourierStyles.Font(
                typeface = font,
                color = blackLightColor,
                sizeInSp = 16
            ),
            read = CourierStyles.Font(
                typeface = font,
                color = blackLightColor,
                sizeInSp = 16
            )
        ),
        timeStyle = CourierStyles.Inbox.TextStyle(
            unread = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 14
            ),
            read = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 14
            )
        ),
        infoViewStyle = CourierStyles.InfoViewStyle(
            font = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 18
            ),
            button = CourierStyles.Button(
                font = CourierStyles.Font(
                    typeface = font,
                    color = whiteColor,
                    sizeInSp = 16
                ),
                backgroundColor = primaryColor,
                cornerRadiusInDp = 100
            )
        ),
        buttonStyle = CourierStyles.Inbox.ButtonStyle(
            unread = CourierStyles.Button(
                font = CourierStyles.Font(
                    typeface = font,
                    color = whiteColor,
                    sizeInSp = 16
                ),
                backgroundColor = primaryColor,
                cornerRadiusInDp = 100
            ),
            read = CourierStyles.Button(
                font = CourierStyles.Font(
                    typeface = font,
                    color = whiteColor,
                    sizeInSp = 16
                ),
                backgroundColor = primaryColor,
                cornerRadiusInDp = 100
            )
        ),
        dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    )
}
```

### Jetpack Compose

```kotlin
CourierInbox(
    modifier = Modifier.padding(innerPadding),
    canSwipePages = true,
    lightTheme = getTheme(context),
    darkTheme = getTheme(context),
    ..
)
```

### Android Layouts

```kotlin
val inbox: CourierInbox = view.findViewById(R.id.courierInbox)
inbox.canSwipePages = true
inbox.lightTheme = getTheme(context)
inbox.darkTheme = getTheme(context)
inbox..
```

&emsp;

### Courier Studio Branding (Optional)

<img width="782" alt="setting" src="https://user-images.githubusercontent.com/6370613/228931428-04dc2130-789a-4ac3-bf3f-0bbb49d5519a.png">

You can control your branding from the [`Courier Studio`](https://app.courier.com/designer/brands).

<table>
    <thead>
        <tr>
            <th width="800px" align="left">Supported Brand Styles</th>
            <th width="200px" align="center">Support</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left"><code>Primary Color</code></td>
            <td align="center">✅</td>
        </tr>
        <tr width="600px">
            <td align="left"><code>Show/Hide Courier Footer</code></td>
            <td align="center">✅</td>
        </tr>
    </tbody>
</table>

---

👋 `Branding APIs` can be found <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Client.md#branding-apis"><code>here</code></a>

&emsp;

## Custom Inbox Example

The raw data you can use to build any UI you'd like.

```kotlin
class CustomInboxFragment: Fragment(R.layout.fragment_custom_inbox) {

    private var inboxListener: CourierInboxListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ..

        lifecycleScope.launch {

            // Allows you to listen to all inbox changes and show a fully custom UI
            inboxListener = Courier.shared.addInboxListener(
                onLoading = {
                    // Called when inbox data is reloaded or refreshed
                },
                onError = { e ->
                    // Called when some error happens
                },
                onUnreadCountChanged = { count ->
                    // Called when the unread inbox message count changes
                },
                onTotalCountChanged = { count, feed ->
                   // Called when the total inbox message count changes for a specific feed
                },
                onMessagesChanged = { messages, canPaginate, feed ->
                    // Called when the inbox messages change for a specific feed
                },
                onPageAdded = { messages, canPaginate, isFirstPage, feed ->
                    // Called when a new inbox messages page is added to a specific feed
                    // This will get called on initial load. Use isFirstPage to handle this case
                },
                onMessageEvent = { message, index, feed, event ->
                    // Called when a message event happens
                    // Message events are: InboxMessageEvent.ADDED, InboxMessageEvent.READ, InboxMessageEvent.UNREAD, InboxMessageEvent.OPENED, InboxMessageEvent.ARCHIVED, InboxMessageEvent.CLICKED
                }
            )
        }
        
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        inboxListener?.remove()
    }

}
```

&emsp;

## Full Examples

<table>
    <thead>
        <tr>
            <th width="800px" align="left">Link</th>
            <th width="200px" align="center">Style</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/app/src/main/java/com/courier/example/fragments/PrebuiltInboxFragment.kt">
                    <code>Default Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md#default-inbox-example">
                    <code>Default</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/app/src/main/java/com/courier/example/fragments/StyledInboxFragment.kt">
                    <code>Styled Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md#styled-inbox-example">
                    <code>Styled</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/app/src/main/java/com/courier/example/fragments/CustomInboxFragment.kt">
                    <code>Custom Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md#custom-inbox-example">
                    <code>Custom</code>
                </a>
            </td>
        </tr>
    </tbody>
</table>

&emsp;

## Available Properties and Functions

```kotlin

// Listen to all inbox events
// Only one "pipe" of data is created behind the scenes for network / performance reasons
lifecycle.coroutineScope.launch.launch {

    // Allows you to listen to all inbox changes and show a fully custom UI
    val inboxListener = Courier.shared.addInboxListener(
        onLoading = {
            // Called when inbox data is reloaded or refreshed
        },
        onError = { e ->
            // Called when some error happens
        },
        onUnreadCountChanged = { count ->
            // Called when the unread inbox message count changes
        },
        onTotalCountChanged = { count, feed ->
           // Called when the total inbox message count changes for a specific feed
        },
        onMessagesChanged = { messages, canPaginate, feed ->
            // Called when the inbox messages change for a specific feed
        },
        onPageAdded = { messages, canPaginate, isFirstPage, feed ->
            // Called when a new inbox messages page is added to a specific feed
            // This will get called on initial load. Use isFirstPage to handle this case
        },
        onMessageEvent = { message, index, feed, event ->
            // Called when a message event happens
            // Message events are: InboxMessageEvent.ADDED, InboxMessageEvent.READ, InboxMessageEvent.UNREAD, InboxMessageEvent.OPENED, InboxMessageEvent.ARCHIVED, InboxMessageEvent.CLICKED
        }
    )
}

// Stop the current listener
inboxListener.remove()

// Remove all listeners
// This will also remove the listener of the prebuilt UI
Courier.shared.removeAllInboxListeners()

// The amount of inbox messages to fetch at a time
// Will affect prebuilt UI
Courier.shared.inboxPaginationLimit = 123

// The available messages the inbox has
val inboxMessages = Courier.shared.inboxMessages

lifecycle.coroutineScope.launch {

    // Fetches the next page of messages
    Courier.shared.fetchNextPageOfMessages()

    // Reloads the inbox
    // Commonly used with pull to refresh
    Courier.shared.refreshInbox()

    // Reads all the messages
    // Writes the update instantly and performs request in background
    try await Courier.shared.readAllInboxMessages()

}

// Inbox Message functions
lifecycle.coroutineScope.launch {
    val messageId = "asdf"
    Courier.shared.openMessage(messageId = messageId)
    Courier.shared.clickMessage(messageId = messageId)
    Courier.shared.readMessage(messageId = messageId)
    Courier.shared.unreadMessage(messageId = messageId)
    Courier.shared.archiveMessage(messageId = messageId)
}

// Extensions
let message = InboxMessage(..)
message.markAsOpened()
message.markAsRead()
message.markAsUnread()
message.markAsClicked()
message.markAsArchived()

// Clicking an action
message.actions?.first?.markAsClicked()
```

---

👋 `Inbox APIs` can be found <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Client.md#inbox-apis"><code>here</code></a>
