# Testing

Common examples you can use to send [`Courier Inbox`](https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md) messages and [`Push Notifications`](https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md) to your users.

&emsp;

## Requirements

<table>
    <thead>
        <tr>
            <th width="300px" align="left">Requirement</th>
            <th width="700px" align="left">Reason</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://app.courier.com/settings/api-keys">
                    <code>Authentication Key</code>
                </a>
            </td>
            <td align="left">
                Needed to authenticate your HTTP requests to the Courier <a href="https://www.courier.com/docs/reference/send/message/"><code>/send</code></a> api
            </td>
        </tr>
    </tbody>
</table>

&emsp;

## Courier Inbox Message

[`Courier Inbox`](https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md) must be setup to receive messages.

<table>
<tr>
<td width="500px" align="left">Swift</td>
<td width="500px" align="left">HTTP</td>
</tr>
<tr width="600px">
<td> 

```swift
try await Courier.shared.sendMessage(
    authKey: "YOUR_AUTH_KEY",
    userId: "example_user_id",
    title: "Hey there 👋",
    message: "Have a great day 😁",
    providers: [.inbox]
)
```

</td>
<td>

```bash
curl --request POST \
  --url https://api.courier.com/send \
  --header 'Authorization: Bearer YOUR_AUTH_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "message": {
        "to": {
            "user_id": "example_user_id"
        },
        "content": {
            "title": "Hey there 👋",
            "body": "Have a great day 😁"
        },
        "routing": {
            "method": "all",
            "channels": [
                "inbox"
            ]
        }
    }
}'
```

</td>
</tr>
</table>

### Result

<img width="471" alt="inbox-example" src="https://user-images.githubusercontent.com/6370613/232109373-2e309171-fdb1-41f1-9652-c8a12c6f9d58.png">

&emsp;

## Push Notification - Apple Push Notification Service (APNS)

[`Push Notifications`](https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md) must be setup to receive messages.

⚠️ Courier automatically applies some overrides. View the overrides [`here`](https://app.courier.com/channels/apn).

<table>
<tr>
<td width="500px" align="left">Swift</td>
<td width="500px" align="left">HTTP</td>
</tr>
<tr width="600px">
<td> 

```swift
try await Courier.shared.sendMessage(
    authKey: "YOUR_AUTH_KEY",
    userId: "example_user_id",
    title: "Hey there 👋",
    message: "Have a great day 😁",
    providers: [.apns]
)
```

</td>
<td>

```bash
curl --request POST \
  --url https://api.courier.com/send \
  --header 'Authorization: Bearer YOUR_AUTH_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "message": {
        "to": {
            "user_id": "example_user_id"
        },
        "content": {
            "title": "Hey there 👋",
            "body": "Have a great day 😁"
        },
        "routing": {
            "method": "all",
            "channels": [
                "apn"
            ]
        }
    }
}'
```

</td>
</tr>
</table>

### Result

<img width="894" alt="apns-push" src="https://user-images.githubusercontent.com/6370613/229195948-1b49b58e-8f38-4fd3-ab6b-7e3844def61d.png">

&emsp;

## Push Notification - Firebase Cloud Messaging (FCM)

[`Push Notifications`](https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md) must be setup to receive messages.

⚠️ Courier automatically applies some overrides. View the overrides [`here`](https://app.courier.com/channels/firebase-fcm).

<table>
<tr>
<td width="500px" align="left">Swift</td>
<td width="500px" align="left">HTTP</td>
</tr>
<tr width="600px">
<td> 

```swift
try await Courier.shared.sendMessage(
    authKey: "YOUR_AUTH_KEY",
    userId: "example_user_id",
    title: "Hey there 👋",
    message: "Have a great day 😁",
    providers: [.fcm]
)
```

</td>
<td>

```bash
curl --request POST \
  --url https://api.courier.com/send \
  --header 'Authorization: Bearer YOUR_AUTH_KEY' \
  --header 'Content-Type: application/json' \
  --data '{
    "message": {
        "to": {
            "user_id": "example_user_id"
        },
        "content": {
            "title": "Hey there 👋",
            "body": "Have a great day 😁"
        },
        "routing": {
            "method": "all",
            "channels": [
                "firebase-fcm"
            ]
        }
    }
}'
```

</td>
</tr>
</table>

### Result

<img width="894" alt="apns-push" src="https://user-images.githubusercontent.com/6370613/229195948-1b49b58e-8f38-4fd3-ab6b-7e3844def61d.png">