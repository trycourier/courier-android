package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.client.CourierApiClient
import com.courier.android.client.CourierClient
import com.courier.android.models.InboxMessage
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

internal object InboxSocketManager {

    private var socketInstance: InboxSocket? = null

    fun getSocketInstance(options: CourierClient.Options): InboxSocket {
        synchronized(this) {
            if (socketInstance == null) {
                socketInstance = InboxSocket(options)
            }
            return socketInstance!!
        }
    }

    fun closeSocket() {
        synchronized(this) {
            socketInstance?.disconnect()
            socketInstance = null
        }
    }

}

class InboxSocket(private val options: CourierClient.Options) : CourierSocket(url = buildUrl(options.clientKey, options.jwt)) {

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

        @SerializedName("click")
        CLICK("click"),

        @SerializedName("unclick")
        UNCLICK("unclick")
    }

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
            val gson = Gson()
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

        private fun buildUrl(clientKey: String?, jwt: String?): String {
            var url = CourierApiClient.INBOX_WEBSOCKET
            url += when {
                jwt != null -> "/?auth=$jwt"
                clientKey != null -> "/?clientKey=$clientKey"
                else -> ""
            }
            return url
        }

    }

}