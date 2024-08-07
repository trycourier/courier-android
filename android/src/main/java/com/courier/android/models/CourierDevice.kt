package com.courier.android.models

import android.os.Build
import com.google.gson.annotations.SerializedName

data class CourierDevice(
    @SerializedName("app_id") val appId: String? = null, // TODO
    @SerializedName("ad_id") val adId: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    val platform: String? = "android",
    val manufacturer: String? = Build.MANUFACTURER,
    val model: String? = Build.MODEL,
)