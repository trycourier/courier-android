package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.models.InboxMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

internal class InboxSocketManager {

    var socket: InboxSocket? = null

    /**
     * Updates the socket instance with new options.
     * Closes the existing socket before creating a new one.
     */
    fun updateInstance(options: CourierClient.Options): InboxSocket {
        closeSocket()
        socket = InboxSocket(options)
        return socket!!
    }

    /**
     * Closes the current socket connection and cleans up resources.
     */
    fun closeSocket() {
        socket?.disconnect()
        socket?.receivedMessage = null
        socket?.receivedMessageEvent = null
        socket = null
    }
}

class InboxSocket(private val options: CourierClient.Options) : CourierSocket(url = buildUrl(options)) {

    enum class PayloadType(val value: String) {
        @SerializedName("event")
        EVENT("event"),

        @SerializedName("message")
        MESSAGE("message")
    }

    enum class EventType(val value: String) {
        @SerializedName("read")
        READ("read"),

        @SerializedName("unread")
        UNREAD("unread"),

        @SerializedName("mark-all-read")
        MARK_ALL_READ("mark-all-read"),

        @SerializedName("opened")
        OPENED("opened"),

        @SerializedName("unopened")
        UNOPENED("unopened"),

        @SerializedName("archive")
        ARCHIVE("archive"),

        @SerializedName("unarchive")
        UNARCHIVE("unarchive"),

        UNKNOWN("unknown");

        // TODO: Support more events in the future (archive_all, archive_read, clicked, etc)

        companion object {
            fun fromValue(value: String): EventType {
                return values().find { it.value == value } ?: UNKNOWN
            }
        }

    }

    private class EventTypeDeserializer: JsonDeserializer<EventType> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EventType {
            return EventType.fromValue(json.asString)
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(EventType::class.java, EventTypeDeserializer())
        .create()

    data class SocketPayload(val type: PayloadType)
    data class MessageEvent(val event: EventType, val messageId: String?, val type: String)

    var receivedMessage: ((InboxMessage) -> Unit)? = null
    var receivedMessageEvent: ((MessageEvent) -> Unit)? = null

    init {
        onMessageReceived = { data ->
            convertToType(data)
        }
    }

    private fun convertToType(data: String) {
        try {
            val payload = gson.fromJson(data, SocketPayload::class.java)
            when (payload.type) {
                PayloadType.EVENT -> {
                    val messageEvent = gson.fromJson(data, MessageEvent::class.java)
                    receivedMessageEvent?.invoke(messageEvent)
                }
                PayloadType.MESSAGE -> {
                    val message = Gson().fromJson(data, InboxMessage::class.java)
                    receivedMessage?.invoke(message)
                }
            }
        } catch (e: Exception) {
            this.onError?.invoke(e)
        }
    }

    suspend fun sendSubscribe(version: Int = 5) {
        val data = mapOf(
            "action" to "subscribe",
            "data" to mutableMapOf(
                "userAgent" to Courier.agent.value(),
                "channel" to options.userId,
                "event" to "*",
                "version" to version,
            ).apply {
                options.connectionId?.let { put("clientSourceId", it) }
                options.clientKey?.let { put("clientKey", it) }
                options.tenantId?.let { put("accountId", it) }
            }
        )
        send(data)
    }

    companion object {

        private fun buildUrl(options: CourierClient.Options): String {
            var url = options.apiUrls.inboxWebSocket
            url += when {
                options.jwt != null -> "/?auth=${options.jwt}"
                options.clientKey != null -> "/?clientKey=${options.clientKey}"
                else -> ""
            }
            return url
        }

    }

}