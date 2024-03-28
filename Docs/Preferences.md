<img width="1000" alt="android-preferences-banner" src="https://github.com/trycourier/courier-android/assets/6370613/686cd3e8-d180-4cbb-9ecb-d847526626ea">

# Courier Preferences

Allow users to update which types of notifications they would like to receive.

## Requirements

<table>
    <thead>
        <tr>
            <th width="300px" align="left">Requirement</th>
            <th width="750px" align="left">Reason</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Authentication.md">
                    <code>Authentication</code>
                </a>
            </td>
            <td align="left">
                Needed to view preferences that belong to a user.
            </td>
        </tr>
    </tbody>
</table>

&emsp;

# Get All User Preferences

Returns all the user's preferences. [`listAllUserPreferences`](https://www.courier.com/docs/reference/user-preferences/list-all-user-preferences/)

```kotlin
// paginationCursor is optional
val preferences = Courier.shared.getUserPreferences()
```

&emsp;

# Update Preference Topic

Updates a specific user preference topic. [`updateUserSubscriptionTopic`](https://www.courier.com/docs/reference/user-preferences/update-subscription-topic-preferences/)

```kotlin
Courier.shared.putUserPreferenceTopic(
    topicId = "6QHD7Z1D4Q436SMECGXENTQYWVQQ",
    status = CourierPreferenceStatus.OPTED_IN,
    hasCustomRouting = true,
    customRouting = listOf(CourierPreferenceChannel.SMS, CourierPreferenceChannel.PUSH)
)
```

&emsp;

# Get Preference Topic

Gets a specific preference topic. [`getUserSubscriptionTopic`](https://www.courier.com/docs/reference/user-preferences/get-subscription-topic-preferences/)

```kotlin
val topic = Courier.shared.getUserPreferenceTopic(
    topicId = "6QHD7Z1D4Q436SMECGXENTQYWVQQ",
)
```
