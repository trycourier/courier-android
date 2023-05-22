package com.courier.android.models

open class CourierChannel(
    val key: String,
    open val data: Map<String, Any>?,
    open val elements: List<CourierElement>
) {
    open val providerOverride: Map<String, Any>? = null
}

data class CourierElement(
    val type: String,
    val content: String,
    val data: Map<String, Any>?
) {

    val map get() = mapOf(
        "type" to type,
        "content" to content,
        "data" to data
    )

}

data class FirebaseCloudMessagingChannel(
    override val data: Map<String, Any>? = null,
    override val elements: List<CourierElement> = emptyList(),
    val aps: Map<String, Any>?,
    val fcmData: Map<String, String>?,
) : CourierChannel(
    key = "firebase-fcm",
    data = data,
    elements = elements
) {

    override val providerOverride get() = mapOf(
        "override" to mapOf(
            "body" to mapOf(
                "data" to fcmData,
                "apns" to mapOf(
                    "payload" to mapOf(
                        "aps" to aps
                    )
                )
            )
        )
    )

}

data class ApplePushNotificationsServiceChannel(
    override val data: Map<String, Any>? = null,
    override val elements: List<CourierElement> = emptyList(),
    val aps: Map<String, Any>?,
) : CourierChannel(
    key = "apn",
    data = data,
    elements = elements
) {

    override val providerOverride get() = mapOf(
        "override" to mapOf(
            "body" to mapOf(
                "aps" to aps
            )
        )
    )

}

data class CourierInboxChannel(
    override val data: Map<String, Any>? = null,
    override val elements: List<CourierElement> = emptyList(),
) : CourierChannel(
    key = "inbox",
    data = data,
    elements = elements
)
