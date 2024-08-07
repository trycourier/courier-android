<img width="1000" alt="android-banner" src="https://github.com/user-attachments/assets/e40f7db0-3b6c-4b5e-a2de-bf6d42b6751d">

&emsp;

# Requirements & Support

<table>
    <thead>
        <tr>
            <th width="880px" align="left">Requirements</th>
            <th width="140px" align="center"></th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">Courier Account</td>
            <td align="center">
                <a href="https://app.courier.com/channels/courier">
                    <code>Sign Up</code>
                </a>
            </td>
        </tr>
        <tr width="600px">
            <td align="left">Minimum Android SDK Version</td>
            <td align="center">
                <code>23</code>
            </td>
        </tr>
    </tbody>
</table>

&emsp;

# Installation

### 1. Add Jitpack repository to your `settings.gradle` file

```gradle
pluginManagement {
    repositories {
        ..
        maven { url 'https://jitpack.io' }
    }
}

dependencyResolutionManagement {
    repositories {
        ..
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add the implementation to your app's `build.gradle` file

```gradle
dependencies {
    implementation 'com.github.trycourier:courier-android:4.2.0' // Groovy
    implementation("com.github.trycourier:courier-android:4.2.0") // Gradle.kts
}
```

### 3. Initialize the SDK (Optional)

If you are only using <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Client.md"><code>CourierClient</code></a> APIs you can skip this step.

```kotlin
// This example is on an Application class
// You can also do this with Activities or Fragments, but
// it is very important to call this before using other 
// parts of the Courier SDK
class YourApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the SDK
        // This is used to give Courier access to SharedPreferences
        // Courier uses SharedPreferences to save some state between app sessions
        // This is important to create a great user experience around push notifications
        Courier.initialize(this)

    }

}
```

&emsp;

# Getting Started

These are all the available features of the SDK.

<table>
    <thead>
        <tr>
            <th width="25px"></th>
            <th width="250px" align="left">Feature</th>
            <th width="725px" align="left">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="center">
                1
            </td>
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Authentication.md">
                    <code>Authentication</code>
                </a>
            </td>
            <td align="left">
                Manages user credentials between app sessions. Required if you would like to use <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md"><code>Inbox</code></a>, <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md"><code>Push Notifications</code></a> and <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Preferences.md"><code>Preferences</code></a>.
            </td>
        </tr>
        <tr width="600px">
            <td align="center">
                2
            </td>
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Inbox.md">
                    <code>Inbox</code>
                </a>
            </td>
            <td align="left">
                An in-app notification center you can use to notify your users. Comes with a prebuilt UI and also supports fully custom UIs.
            </td>
        </tr>
        <tr width="600px">
            <td align="center">
                3
            </td>
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/PushNotifications.md">
                    <code>Push Notifications</code>
                </a>
            </td>
            <td align="left">
                Automatically manages push notification device tokens and gives convenient functions for handling push notification receiving and clicking.
            </td>
        </tr>
        <tr width="600px">
            <td align="center">
                4
            </td>
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Preferences.md">
                    <code>Preferences</code>
                </a>
            </td>
            <td align="left">
                Allow users to update which types of notifications they would like to receive.
            </td>
        </tr>
        <tr width="600px">
            <td align="center">
                5
            </td>
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/blob/master/Docs/Client.md">
                    <code>CourierClient</code>
                </a>
            </td>
            <td align="left">
                The base level API wrapper around the Courier endpoints. Useful if you have a highly customized user experience or codebase requirements.
            </td>
        </tr>
    </tbody>
</table>

&emsp;

# Proguard

Here is what the Courier Android SDK automatically handles with Proguard:

<a href="https://github.com/trycourier/courier-android/blob/master/android/consumer-rules.pro">
    <code>Proguard Rules</code>
</a>

&emsp;

# Example Projects

Starter projects using this SDK.

<table>
    <thead>
        <tr>
            <th width="800px" align="left">Project Link</th>
            <th width="200px" align="center">Language</th>
        </tr>
    </thead>
    <tbody>
        <tr width="600px">
            <td align="left">
                <a href="https://github.com/trycourier/courier-android/tree/master/app">
                    <code>Example</code>
                </a>
            </td>
            <td align="center"><code>Kotlin</code></td>
        </tr>
    </tbody>
</table>

&emsp;

## **Share feedback with Courier**

We want to make this the best SDK for managing notifications! Have an idea or feedback about our SDKs? Here are some links to contact us:

- [Courier Feedback](https://feedback.courier.com/)
- [Courier Android Issues](https://github.com/trycourier/courier-android/issues)
