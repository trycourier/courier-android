<!-- Generated from https://www.courier.com/docs/in-app/build-a-custom-inbox by mintlify-docs. Edit the docs source, not this file. -->

# Build a custom inbox

> Swap parts of the inbox for your own renderers on the web SDKs, or build the whole UI on any platform from the SDK's messages, unread counts, and actions.

Source: https://www.courier.com/docs/in-app/build-a-custom-inbox

Two levels of custom rendering, in increasing order of control.

**Replace parts** of the built-in inbox, its list item, header, or states, while it keeps managing the list, sync, and pagination. Or **build your own UI** from the message data and actions directly, which every platform supports including mobile.

Reach for the first when the default inbox is nearly right and one element isn't. Reach for the second when your app already has a list component you want to keep. [Customize the inbox](https://www.courier.com/docs/in-app/customize-the-inbox) covers theming, which handles most cases without either.

## Build your own UI

You don't have to render `CourierInbox` at all. Every SDK exposes the message data and actions directly, so you can build the whole UI while the SDK keeps everything synced. The setup is the same everywhere:

1. Start the feed.
2. Render your own UI from the messages.
3. Toggle read (or archive) on interaction.
4. Tear down the listener when the view goes away.

The one difference is how the feed starts:

- **Mobile SDKs**: adding an inbox listener loads the first page for you, if you have [signed the user in](https://www.courier.com/docs/in-app/add-an-inbox) first. (`refreshInbox()` is only for pull-to-refresh.)

```kotlin
// 1. Start: adding a listener loads the first page (sign in first).
//    addInboxListener is a suspend function; call it from a coroutine.
lifecycleScope.launch {
  val listener = Courier.shared.addInboxListener(
    onMessagesChanged = { messages, canPaginate, feed ->
      render(messages) // 2. Build your own UI
    }
  )
  // 4. Tear down when the view goes away: listener.remove()
}

// 3. Toggle read on tap
if (message.isRead) message.markAsUnread() else message.markAsRead()
```

See [Data and actions](https://www.courier.com/docs/in-app/build-a-custom-inbox#data-and-actions) for the full surface. On the mobile SDKs and Web Components, remove the listener when your view is torn down.

## Data and actions

The pattern above uses a subset of what each SDK exposes. Here is the full surface: the **data** you read (messages, unread count, pagination state) and the **actions** you call (mark read, archive, paginate, refresh). The built-in inbox calls these for you.

On mobile, an inbox listener pushes the data to you and the actions take a `messageId`.

```kotlin
// Subscribe for live data (addInboxListener is a suspend function)
lifecycleScope.launch {
  val listener = Courier.shared.addInboxListener(
    onLoading = { isRefresh -> /* spinner */ },
    onError = { error -> /* handle */ },
    onUnreadCountChanged = { count -> /* badge */ },
    onTotalCountChanged = { totalCount, feed -> /* count */ },
    onMessagesChanged = { messages, canPaginate, feed -> /* render */ },
    onPageAdded = { messages, canPaginate, isFirstPage, feed -> /* append */ },
    onMessageEvent = { message, index, feed, event -> /* InboxMessageEvent.ADDED / .READ ... */ }
  )
}

// DATA (values you read)

// Messages in the main feed
Courier.shared.feedMessages          // List<InboxMessage>
// Messages in the archived feed
Courier.shared.archivedMessages      // List<InboxMessage>
// Current page size (1..100)
Courier.shared.inboxPaginationLimit
// Each InboxMessage: messageId, title, body, preview, subtitle,
// isRead, isOpened, isArchived, created, actions

// ACTIONS (suspend; take a messageId)

// Mark read / unread
Courier.shared.readMessage(messageId)
Courier.shared.unreadMessage(messageId)
// Track click / open
Courier.shared.clickMessage(messageId)
Courier.shared.openMessage(messageId)
// Archive (no unarchive on Android)
Courier.shared.archiveMessage(messageId)
// Mark every message read
Courier.shared.readAllInboxMessages()
// Set the page size
Courier.shared.inboxPaginationLimit = 50
// Load the next page (.FEED or .ARCHIVE)
Courier.shared.fetchNextInboxPage(InboxMessageFeed.FEED)
// Refresh from the server
Courier.shared.refreshInbox()
// Tear down and reconnect
Courier.shared.restartInbox()
// Stop the connection
Courier.shared.closeInbox()
// Stop listening (cleanup)
listener.remove()
// Or from a message:
// markAsRead() / markAsUnread() / markAsOpened() / markAsClicked() / markAsArchived()
// Or from one of its actions:
// message.actions?.firstOrNull()?.markAsClicked()
```
