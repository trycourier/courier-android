<!-- Generated from https://www.courier.com/docs/in-app/add-toasts by mintlify-docs. Edit the docs source, not this file. -->

# Show an alert when a new inbox message arrives

> There is no built-in toast component on mobile. Listen for new inbox messages and show your own UI, or send a push notification.

Source: https://www.courier.com/docs/in-app/add-toasts

New-message alerts use the inbox's feed and [authentication](https://www.courier.com/docs/in-app/authenticate-users).

> **Note:** The built-in toast is web-only. On iOS, Android, Flutter, and React Native, listen for new messages and show your own UI. Or use [push notifications](https://www.courier.com/docs/integrations/push/apple-push-notification).

## Set up

Install the SDK for your framework:

```bash
# In settings.gradle add the JitPack repo, then the dependency in build.gradle:
# maven { url 'https://jitpack.io' }
# implementation 'com.github.trycourier:courier-android:<version>'
```

On mobile, the SDK also needs native project setup before it renders: [iOS](https://www.courier.com/docs/sdk-libraries/ios#installation), [Android](https://www.courier.com/docs/sdk-libraries/android#installation), [Flutter](https://www.courier.com/docs/sdk-libraries/flutter#installation), and [React Native](https://www.courier.com/docs/sdk-libraries/react-native#installation).

Then [authenticate the user](https://www.courier.com/docs/in-app/authenticate-users) with the same `signIn` as the inbox. On mobile, listen for new messages and show your own UI:

```kotlin
// No built-in toast on Android: listen and show your own UI
// addInboxListener is a suspend function; call it from a coroutine
lifecycleScope.launch {
  val listener = Courier.shared.addInboxListener(
    onMessageEvent = { message, index, feed, event ->
      if (event == InboxMessageEvent.ADDED) {
        showMyToast(message) // your own toast UI
      }
    }
  )
}
```
