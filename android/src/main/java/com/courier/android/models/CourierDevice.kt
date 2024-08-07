package com.courier.android.models

import android.content.Context
import android.os.Build
import com.google.gson.annotations.SerializedName

data class CourierDevice(
    @SerializedName("app_id") val appId: String? = null,
    @SerializedName("ad_id") val adId: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    val platform: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
) {

    companion object {

        fun current(context: Context): CourierDevice {
            return CourierDevice(
                appId = context.packageName,
                adId = null,
                deviceId = null,
                platform = "android",
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
            )
        }

    }

}