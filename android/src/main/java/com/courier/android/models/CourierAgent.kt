package com.courier.android.models

sealed class CourierAgent(val version: String) {

    class NativeIOS(version: String) : CourierAgent(version)
    class ReactNativeIOS(version: String) : CourierAgent(version)
    class FlutterIOS(version: String) : CourierAgent(version)

    fun value(): String {
        return when (this) {
            is NativeIOS -> "courier-ios/$version"
            is ReactNativeIOS -> "courier-react-native-ios/$version"
            is FlutterIOS -> "courier-flutter-ios/$version"
        }
    }

    internal val isReactNative: Boolean
        get() = this is ReactNativeIOS

}