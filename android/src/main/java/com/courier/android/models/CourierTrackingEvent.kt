package com.courier.android.models

enum class CourierTrackingEvent(val value: String) {
    CLICKED("CLICKED"),
    DELIVERED("DELIVERED"),
    OPENED("OPENED"),
    READ("READ"),
    UNREAD("UNREAD")
}