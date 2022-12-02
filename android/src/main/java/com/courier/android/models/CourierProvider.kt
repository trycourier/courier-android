package com.courier.android.models

enum class CourierProvider(val value: String) {
    APNS("apn"),
    FCM("firebase-fcm"),
//    EXPO("expo"),
//    ONE_SIGNAL("onesignal")
}

// Creates the properly formatted payload needed to send to APNS and/or FCM
internal fun CourierProvider.override(title: String, body: String, isProduction: Boolean): Map<String, Any> {

    return when (this) {
        CourierProvider.APNS -> mapOf(
            "override" to mapOf(
                "config" to mapOf(
                    "isProduction" to isProduction
                ),
                "body" to mapOf(
                    "mutable-content" to 1
                )
            )
        )
        CourierProvider.FCM -> mapOf(
            "override" to mapOf(
                "body" to mapOf(
                    "notification" to null,
                    "data" to mapOf(
                        "title" to title,
                        "body" to body
                    ),
                    "apns" to mapOf(
                        "payload" to mapOf(
                            "aps" to mapOf(
                                "mutable-content" to 1,
                                "alert" to mapOf(
                                    "title" to title,
                                    "body" to body,
                                ),
                                "sound" to "bingbong.aiff"
                            )
                        )
                    )
                )
            )
        )
    }

}

