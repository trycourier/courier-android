package com.courier.android.socket

import com.courier.android.models.InboxMessage
import com.courier.android.repositories.Repository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

internal class InboxSocket(private val clientKey: String?, private val jwt: String?, onClose: (code: Int, reason: String?) -> Unit, onError: (e: Exception) -> Unit) : CourierSocket(url = buildUrl(clientKey, jwt), onClose = onClose, onError = onError) {

    enum class PayloadType(val value: String) {
        @SerializedName("event") EVENT("event"),
        @SerializedName("message") MESSAGE("message")
    }

    enum class EventType(val value: String) {
        @SerializedName("read") READ("read"),
        @SerializedName("unread") UNREAD("unread"),
        @SerializedName("mark-all-read") MARK_ALL_READ("mark-all-read"),
        @SerializedName("opened") OPENED("opened")
    }

    data class SocketPayload(val type: PayloadType, val event: EventType?)
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
            this.onError(e)
        }
    }

    suspend fun sendSubscribe(userId: String, tenantId: String?, clientSourceId: String, version: Int = 5) {
        val data = mutableMapOf(
            "action" to "subscribe",
            "data" to mutableMapOf(
                "channel" to userId,
                "event" to "*",
                "version" to version,
                "clientSourceId" to clientSourceId
            ).apply {
                clientKey?.let { put("clientKey", it) }
                tenantId?.let { put("accountId", it) }
            }
        )
        send(data)
    }

    companion object {

        private fun buildUrl(clientKey: String?, jwt: String?): String {
            var url = Repository.inboxWebSocket
            url += when {
                jwt != null -> "/?auth=$jwt"
                clientKey != null -> "/?clientKey=$clientKey"
                else -> ""
            }
            return url
        }

    }

}