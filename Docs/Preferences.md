<img width="1000" alt="android-preferences-banner" src="https://github.com/trycourier/courier-android/assets/6370613/686cd3e8-d180-4cbb-9ecb-d847526626ea">

# Courier Preferences

In-app notification settings that allow your users to customize which of your notifications they receive. Allows you to build high quality, flexible preference settings very quickly.

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

## Default Preferences View

The default `CourierPreferences` styles.

<img width="296" alt="default-inbox-styles" src="https://github.com/trycourier/courier-android/assets/6370613/681a8de9-536a-40cb-93bf-8e51caae7f4b.gif">

```kotlin
// Get the view by id
val preferences: CourierPreferences = view.findViewById(R.id.courierPreferences)

// Set the mode and listen to errors
preferences.apply {
    mode = CourierPreferences.Mode.Topic
    onError = { e ->
        print(e)
    }
}
```

&emsp;

## Styled Preferences View

The styles you can use to quickly customize the `CourierPreferences`.

<img width="296" alt="default-inbox-styles" src="https://github.com/trycourier/courier-android/assets/6370613/67c6b772-f6e3-4937-b090-b0769d64d100.gif">

```kotlin
val preferences: CourierPreferences = view.findViewById(R.id.courierPreferences)

preferences.apply {
    
    val availableChannels = listOf(
        CourierPreferenceChannel.PUSH,
        CourierPreferenceChannel.SMS,
        CourierPreferenceChannel.EMAIL
    )
    
    mode = CourierPreferences.Mode.Channels(availableChannels)
    
    onError = { e ->
        print(e)
    }
    
}

val font = ResourcesCompat.getFont(requireContext(), R.font.poppins)
val purple500 = ContextCompat.getColor(requireContext(), R.color.courier_purple)
val purple200 = ContextCompat.getColor(requireContext(), R.color.purple_200)
val grey500 = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

val theme = CourierPreferencesTheme(
    brandId = "7S9RBWHHS9MBYRPSRYAFYF9K3Q1M", // Optional
    loadingIndicatorColor = purple500,
    sectionTitleFont = CourierStyles.Font(
        typeface = font,
        color = purple500,
        sizeInSp = 22,
    ),
    topicDividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
    topicTitleFont = CourierStyles.Font(
        typeface = font,
    ),
    topicSubtitleFont = CourierStyles.Font(
        typeface = font,
        color = grey500,
    ),
    sheetTitleFont = CourierStyles.Font(
        typeface = font,
        color = purple500,
        sizeInSp = 22,
    ),
    sheetDividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
    sheetSettingStyles = CourierStyles.Preferences.SettingStyles(
        font = CourierStyles.Font(
            typeface = font,
        ),
        toggleThumbColor = purple500,
        toggleTrackColor = purple200,
    )
)

preferences.lightTheme = theme
preferences.darkTheme = theme
```

&emsp;

### Courier Studio Branding (Optional)

<img width="782" alt="setting" src="https://user-images.githubusercontent.com/6370613/228931428-04dc2130-789a-4ac3-bf3f-0bbb49d5519a.png">

You can control your branding from the [`Courier Studio`](https://app.courier.com/designer/brands).

<table>
    <thead>
        <tr>
            <th width="850px" align="left">Supported Brand Styles</th>
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

👋 `Preference APIs` can be found <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Client.md#preferences-apis"><code>here</code></a>
