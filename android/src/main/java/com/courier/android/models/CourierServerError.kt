package com.courier.android.models

internal data class CourierServerError(
    val type: String,
    val message: String
) {
    val toException: CourierException get() {
        return CourierException("$type: $message")
    }
}