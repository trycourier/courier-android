package com.courier.android.models

enum class CourierPushEvent(val value: String) {
    CLICKED("CLICKED"),
    DELIVERED("DELIVERED"),
    OPENED("OPENED"),
    READ("READ"),
    UNREAD("UNREAD")
}