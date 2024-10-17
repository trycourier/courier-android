package com.courier.android.models

sealed class CourierAgent(val version: String) {

    class NativeAndroid(version: String) : CourierAgent(version)
    class ReactNativeAndroid(version: String) : CourierAgent(version)
    class FlutterAndroid(version: String) : CourierAgent(version)

    fun value(): String {
        return when (this) {
            is NativeAndroid -> "courier-android/$version"
            is ReactNativeAndroid -> "courier-react-native-android/$version"
            is FlutterAndroid -> "courier-flutter-android/$version"
        }
    }

    val isReactNative get() = this is ReactNativeAndroid

}