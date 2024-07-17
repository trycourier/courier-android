package com.courier.android.models

import android.os.Build

internal enum class CourierPlatform(val platform: String) {
    IOS("ios"),
    ANDROID("android")
}

data class CourierDevice(
    val app_id: String?,
    val ad_id: String?,
    val device_id: String?,
    val platform: String?,
    val manufacturer: String?,
    val model: String?
) {

    companion object {

        val current = CourierDevice(
            app_id = null, // Paused due to potential static leaking and additional params
            ad_id = null, // Requires Google Play services
            device_id = null, // Not consistent way to tell a UUID
            platform = CourierPlatform.ANDROID.platform,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )

    }

}