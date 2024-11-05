# Authentication

Manages user credentials between app sessions.

&emsp;

## SDK Features that need Authentication

<table>
    <thead>
        <tr>
            <th width="250px" align="left">Feature</th>
            <th width="750px" align="left">Reason</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md">
                    <code>Inbox</code>
                </a>
            </td>
            <td align="left">
                Needs Authentication to view inbox messages that belong to a user.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md">
                    <code>Push Notifications</code>
                </a>
            </td>
            <td align="left">
                Needs Authentication to sync push notification device tokens to the current user and Courier.
            </td>
        </tr>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Preferences.md">
                    <code>Preferences</code>
                </a>
            </td>
            <td align="left">
                Needs Authentication to change user notification preferences.
            </td>
        </tr>
    </tbody>
</table>

&emsp;

# Getting Started

Put this code where you normally manage your user's state. The user's access to [`Inbox`](https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md), [`Push Notifications`](https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md) and [`Preferences`](https://github.com/trycourier/courier-android/blob/master/Docs/Preferences.md) will automatically be managed by the SDK and stored in persistent storage. This means that if your user fully closes your app and starts it back up, they will still be "signed in".

⚠️ Be sure to call `Courier.initialize(context)` before `Courier.shared.signIn(...)`. [`Click here`](https://github.com/trycourier/courier-android#3-initialize-the-sdk) for more details.

&emsp;

## 1. Generate a JWT

To generate a JWT, you will need to:
1. Create an endpoint on your backend
2. Call this function inside that endpoint: [`Generate Auth Tokens`](https://www.courier.com/docs/reference/auth/issue-token/)
3. Return the JWT

Here is a curl example with all the scopes needed that the SDK uses. Change the scopes to the scopes you need for your use case.

```curl
curl --request POST \
     --url https://api.courier.com/auth/issue-token \
     --header 'Accept: application/json' \
     --header 'Authorization: Bearer $YOUR_AUTH_KEY' \
     --header 'Content-Type: application/json' \
     --data
 '{
    "scope": "user_id:$YOUR_USER_ID write:user-tokens inbox:read:messages inbox:write:events read:preferences write:preferences read:brands",
    "expires_in": "$YOUR_NUMBER days"
  }'
```

## 2. Get a JWT in your app

```kotlin
lifecycleScope.launch {
    let userId = "your_user_id"
    let jwt = YourBackend.generateCourierJWT(for: userId)
}
```

## 3. Sign your user in

Signed in users will stay signed in between app sessions.

```kotlin
lifecycleScope.launch {
    let userId = "your_user_id"
    Courier.shared.signIn(accessToken = jwt, userId = userId)
}
```

If the token is expired, you can generate a new one from your endpoint and call `Courier.shared.signIn(...)` again. You will need to check the token manually for expiration or generate a new one when the user views a specific screen in your app. It is up to you to handle token expiration and refresh based on your security needs.

## 4. Sign your user out

This will remove any credentials that are stored between app sessions.

```swift
lifecycleScope.launch {
    Courier.shared.signOut()
}
```

## All Available Authentication Values

```swift
val userId = Courier.shared.userId
val isUserSignedIn = Courier.shared.isUserSignedIn

val listener = Courier.shared.addAuthenticationListener { userId ->
    print(userId ?: "No userId found")
}

listener.remove()
```

More Info: [`Courier Issue Token Docs`](https://www.courier.com/docs/reference/auth/issue-token/)

This request should exist in a separate endpoint served by your backend.
