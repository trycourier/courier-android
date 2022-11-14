# Courier Android Contribution Guide

## Getting Started

1. Clone the repo and open `courier-android` with Android Studio
2. Open terminal navigate to root directory run 

```bash 
sh env-setup.sh
```

3. Navigate to `app/java/com/courier/example/Env.kt`
4. Provide your FCM (Firebase Cloud Messaging) and Courier credentials
	- [Firebase Setup](https://firebase.google.com/docs/android/setup#console)
	- [Courier API Keys](https://app.courier.com/settings/api-keys)
	- [Courier JWT](https://www.courier.com/docs/reference/auth/issue-token/)

From here, you are all set to start working on the package! 🙌

## Testing, Debugging & Release

While developing, you can run the project from Android Studio to test your changes. To see any changes you make in your library code will be reflected in the example app everytime you rebuild the app.

To make package changes:
1. Edit code inside of the `android` directory

To test the package changes in the example app:
1. Run `app`

To run automated tests:
1. Run `android/src/androidTest/com/courier/android/CourierTests`
	- Requires proper `Env.kt` settings

To release a new build of the SDK:
1. Change the `VERSION` of `android/java/com/courier/android/Courier.kt` to the SDK value you'd like to use
2. Run `sh release.sh` from root
	- Required access to create builds in Github with Github CLI
