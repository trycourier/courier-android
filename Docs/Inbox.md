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

&emsp;

## Default Inbox Example

The default `CourierInbox` styles.

<img width="810" alt="android-default-inbox-styles" src="https://github.com/trycourier/courier-android/assets/6370613/1aca0cd0-a0bb-4e22-9ac6-65a97a06f914">

```kotlin
val inbox: CourierInbox = view.findViewById(R.id.courierInbox)

inbox.setOnClickMessageListener { message, index ->
    Courier.log(message.toString())
    if (message.isRead) message.markAsUnread() else message.markAsRead()
}

inbox.setOnClickActionListener { action, message, index ->
    Courier.log(action.toString())
}
```

&emsp;

## Styled Inbox Example

The styles you can use to quickly customize the `CourierInbox`.

<img width="415" alt="android-styled-inbox-styles" src="https://github.com/trycourier/courier-android/assets/6370613/cfea668d-2a8d-4da6-a128-bf5d747a8e11">

```kotlin
val inbox: CourierInbox = view.findViewById(R.id.courierInbox)

val theme = CourierInboxTheme(
    unreadIndicatorBarColor = ContextCompat.getColor(requireContext(), R.color.courier_red),
    loadingIndicatorColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
    titleFont = CourierInboxFont(
        typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins),
        color = ContextCompat.getColor(requireContext(), android.R.color.black),
        sizeInSp = 18
    ),
    bodyFont = CourierInboxFont(
        typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins),
        color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
        sizeInSp = 16
    ),
    timeFont = CourierInboxFont(
        typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins),
        color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
        sizeInSp = 14
    ),
    detailTitleFont = CourierInboxFont(
        typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins),
        color = ContextCompat.getColor(requireContext(), android.R.color.black),
        sizeInSp = 18
    ),
    buttonStyles = CourierInboxButtonStyles(
        font = CourierInboxFont(
            typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins),
            color = ContextCompat.getColor(requireContext(), android.R.color.white),
            sizeInSp = 16
        ),
        backgroundColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
        cornerRadiusInDp = 100
    ),
    dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
)

inbox.lightTheme = theme
inbox.darkTheme = theme

inbox.setOnClickMessageListener { message, index ->
    Courier.log(message.toString())
    if (message.isRead) message.markAsUnread() else message.markAsRead()
}

inbox.setOnClickActionListener { action, message, index ->
    Courier.log(action.toString())
}

inbox.setOnScrollInboxListener { offsetInDp ->
    Courier.log(offsetInDp.toString())
}
```

&emsp;

### Courier Studio Branding (Optional)

<img width="782" alt="setting" src="https://user-images.githubusercontent.com/6370613/228931428-04dc2130-789a-4ac3-bf3f-0bbb49d5519a.png">

You can control your branding from the [`Courier Studio`](https://app.courier.com/designer/brands).

```kotlin
// Sets the brand for the inbox
Courier.shared.inboxBrandId = "YOUR_BRAND_ID"

val inbox: CourierInbox = view.findViewById(R.id.courierInbox)

// ⚠️ Any colors you apply to the theme will override the brand you applied
val theme = CourierInboxTheme(
    loadingIndicatorColor = ContextCompat.getColor(requireContext(), R.color.courier_purple)
)

inbox.lightTheme = theme
inbox.darkTheme = theme
```

&emsp;

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

&emsp;

## Custom Inbox Example

The raw data you can use to build whatever UI you'd like.

<img width="415" alt="android-custom-inbox" src="https://github.com/trycourier/courier-android/assets/6370613/e89a3b52-08ba-426c-94ac-ee26bcf7cfee">

```kotlin
class CustomInboxFragment: Fragment(R.layout.fragment_custom_inbox) {

    private lateinit var stateTextView: TextView
    private lateinit var inboxListener: CourierInboxListener
    private lateinit var recyclerView: RecyclerView

    private val messagesAdapter = MessagesAdapter()
    private val loadingAdapter = LoadingAdapter()
    private val adapter = ConcatAdapter(messagesAdapter)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stateTextView = view.findViewById(R.id.stateTextView)

        // Get the menu
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            return@setOnMenuItemClickListener if (item.itemId == R.id.readAll) {

                Courier.shared.readAllInboxMessages(
                    onSuccess = {
                        print("Messages are read")
                    },
                    onFailure = { error ->
                        print(error)
                    }
                )

                true
            } else {
                false
            }
        }

        // Create the list
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        // Handle pull to refresh
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {

            Courier.shared.refreshInbox {
                refreshLayout.isRefreshing = false
            }

        }

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {

                stateTextView.isVisible = false

                refreshAdapters(
                    showLoading = true
                )

            },
            onError = { e ->

                stateTextView.text = e.message
                stateTextView.isVisible = true

                print(e)
                refreshAdapters()

            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->

                stateTextView.isVisible = messages.isEmpty()
                stateTextView.text = "No messages found"

                refreshAdapters(
                    showMessages = messages.isNotEmpty(),
                    showLoading = canPaginate
                )

            }
        )

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshAdapters(showMessages: Boolean = false, showLoading: Boolean = false) {
        if (showMessages) adapter.addAdapter(0, messagesAdapter) else adapter.removeAdapter(messagesAdapter)
        if (showLoading) adapter.addAdapter(loadingAdapter) else adapter.removeAdapter(loadingAdapter)
        messagesAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        inboxListener.remove()
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
                <a href="https://github.com/trycourier/courier-ios/blob/master/Example/Example/PrebuiltInboxViewController.swift">
                    <code>Default Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-ios/blob/master/Docs/Inbox.md#default-inbox-example">
                    <code>Default</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-ios/blob/master/Example/Example/StyledInboxViewController.swift">
                    <code>Styled Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-ios/blob/master/Docs/Inbox.md#styled-inbox-example">
                    <code>Styled</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-ios/blob/master/Example/Example/CustomInboxViewController.swift">
                    <code>Custom Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-ios/blob/master/Docs/Inbox.md#custom-inbox-example">
                    <code>Custom</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-ios/blob/master/SwiftUI-Example/SwiftUI-Example/ContentView.swift">
                    <code>SwiftUI Example</code>
                </a>
            </td>
            <td align="center">
                <a href="https://github.com/trycourier/courier-ios/blob/master/Docs/Inbox.md#styled-inbox-example">
                    <code>Styled</code>
                </a>
            </td>
        </tr>
    </tbody>
</table>

&emsp;

## Available Properties and Functions

```swift
import Courier_iOS

// Listen to all inbox events
// Only one "pipe" of data is created behind the scenes for network / performance reasons
let inboxListener = Courier.shared.addInboxListener(
    onInitialLoad: {
        // Called when the inbox starts up
    },
    onError: { error in
        // Called if an error occurs
    },
    onMessagesChanged: { messages, unreadMessageCount, totalMessageCount, canPaginate in
        // Called when messages update
    }
)

// Stop the current listener
inboxListener.remove()

// Remove all listeners
// This will also remove the listener of the prebuilt UI
Courier.shared.removeAllInboxListeners()

// The amount of inbox messages to fetch at a time
// Will affect prebuilt UI
Courier.shared.inboxPaginationLimit = 123

// The available messages the inbox has
let inboxMessages = Courier.shared.inboxMessages

Task {

    // Fetches the next page of messages
    try await Courier.shared.fetchNextPageOfMessages()

    // Reloads the inbox
    // Commonly used with pull to refresh
    try await Courier.shared.refreshInbox()

    // Reads / Unreads a message
    // Writes the update instantly and performs request in background
    try await Courier.shared.readMessage(messageId: "1-321...")
    try await Courier.shared.unreadMessage(messageId: "1-321...")

    // Reads all the messages
    // Writes the update instantly and performs request in background
    try await Courier.shared.readAllInboxMessages()

}

// Mark message as read/unread
let message = InboxMessage(...)

// Calls Courier.shared.un/readMessage(messageId...) under the hood
// Has optional callbacks
message.markAsRead()
message.markAsUnread()

```
