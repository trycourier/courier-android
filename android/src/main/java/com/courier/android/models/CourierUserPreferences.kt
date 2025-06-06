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

        val allCases: List<CourierPreferenceStatus>
            get() = listOf(OPTED_IN, OPTED_OUT, REQUIRED)

    }

    val title: String
        get() {
            return when (this) {
                OPTED_IN -> "Opted In"
                OPTED_OUT -> "Opted Out"
                REQUIRED -> "Required"
                UNKNOWN -> "Unknown"
            }
        }


}

enum class CourierPreferenceChannel(val value: String) {

    DIRECT_MESSAGE("direct_message"),
    INBOX("inbox"),
    EMAIL("email"),
    PUSH("push"),
    SMS("sms"),
    WEBHOOK("webhook"),
    UNKNOWN("unknown");

    companion object {

        fun fromString(value: String): CourierPreferenceChannel {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }

        val allCases: List<CourierPreferenceChannel>
            get() = listOf(INBOX, PUSH, SMS, EMAIL, DIRECT_MESSAGE, WEBHOOK)

    }

    val title: String
        get() {
            return when (this) {
                DIRECT_MESSAGE -> "In App Messages"
                INBOX -> "Inbox"
                EMAIL -> "Emails"
                PUSH -> "Push Notifications"
                SMS -> "Text Messages"
                WEBHOOK -> "Webhooks"
                UNKNOWN -> "Unknown"
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
    @SerializedName("topic_name") val topicName: String,
    @SerializedName("section_name") val sectionName: String,
    @SerializedName("section_id") val sectionId: String
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