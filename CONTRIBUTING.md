# Courier Android Contribution Guide

## Getting Started

1. Clone the repo and open `courier-android` with Android Studio
2. Open terminal navigate to root directory run 

```bash 
sh env_setup.sh
```

3. Navigate to `app/src/main/java/com/courier/example/Env.kt` (and `android/src/androidTest/java/com/courier/app/Env.kt` for instrumentation tests)
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
1. JVM unit tests: `./gradlew :android:testDebugUnitTest` (sources under `android/src/test/`)
2. Instrumentation tests: run on a device/emulator — `./gradlew :android:connectedDebugAndroidTest` (sources under `android/src/androidTest/`). Requires `Env.kt` under `android/src/androidTest/java/com/courier/app/`.

To release a new build of the SDK:
1. Change the `VERSION` in `android/src/main/java/com/courier/android/Courier.kt` to the SDK value you'd like to use
2. Use the scripts under `scripts/` (or `deploy.sh`) from root
	- Required access to create builds in Github with Github CLI
	- Release are distributed via Jitpack.io. You can check on the progress of the release [here](https://jitpack.io/#trycourier/courier-android)
