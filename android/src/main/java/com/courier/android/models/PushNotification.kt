package com.courier.android.models

data class CourierMessage(
    val title: String?,
    val body: String?,
    val data: Map<String, String>
)

interface CourierPushHandler {
    fun showNotification(msg: CourierMessage) {}
}