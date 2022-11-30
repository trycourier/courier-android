package com.courier.android.models

import org.json.JSONObject

interface ProviderPayload {
    fun generatePayload(title: String, body: String): JSONObject
}

enum class CourierProvider(val value: String) : ProviderPayload {

    APNS("apn") {

        override fun generatePayload(title: String, body: String): JSONObject {

            return JSONObject(
                mapOf(
                    "payload" to mapOf(
                        "aps" to mapOf(
                            "mutable-content" to 1,
                            "alert" to mapOf(
                                "title" to title,
                                "body" to body
                            ),
                            "sound" to "bingbong.aiff"
                        )
                    )
                )
            )

        }

    },

    FCM("firebase-fcm") {

        override fun generatePayload(title: String, body: String): JSONObject {

            return JSONObject(
                mapOf(
                    "title" to title,
                    "body" to body
                )
            )

        }

    },

//    EXPO("expo"),
//    ONE_SIGNAL("onesignal")
}

