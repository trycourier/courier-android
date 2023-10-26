package com.courier.android.models

import com.google.gson.annotations.SerializedName

enum class CourierPreferenceStatus(val value: String) {

    OPTED_IN("OPTED_IN"),
    OPTED_OUT("OPTED_OUT"),
    REQUIRED("REQUIRED"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromString(value: String): CourierPreferenceStatus {
            return CourierPreferenceStatus.values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }

}

enum class CourierPreferenceChannel(val value: String) {

    DIRECT_MESSAGE("direct_message"),
    EMAIL("email"),
    PUSH("push"),
    SMS("sms"),
    WEBHOOK("webhook"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): CourierPreferenceChannel {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }

}

data class CourierUserPreferences(
    val items: List<CourierPreferenceTopic>,
    val paging: Paging
)

data class CourierPreferenceTopic(
    @SerializedName("default_status") val defaultStatus: CourierPreferenceStatus,
    @SerializedName("has_custom_routing") val hasCustomRouting: Boolean,
    private val custom_routing: List<String>,
    val status: CourierPreferenceStatus,
    @SerializedName("topic_id") val topicId: String,
    @SerializedName("topic_name") val topicName: String
) {
    val customRouting: List<CourierPreferenceChannel> get() = custom_routing.map { CourierPreferenceChannel.fromString(it) }
}

data class Paging(
    val cursor: String?,
    val more: Boolean
)

data class CourierUserPreferencesTopic(
    val topic: CourierPreferenceTopic
)