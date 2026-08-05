package com.courier.android.socket

import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.models.InboxMessage
import com.courier.android.utils.log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.UUID

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

    /**
     * A v2 frame. Messages arrive as `{"event":"message","iwpv":"v2","data":{…}}`; anything else
     * is a message event carrying its own `event` name at the root.
     */
    internal data class MessageFrame(val event: String?, val iwpv: String?, val data: InboxMessage?)

    /**
     * Server control frames: `{"tid":…,"response":"ack"|"pong"|"config"}` and
     * `{"iwpv":…,"tid":…,"action":"ping"}`.
     *
     * Answering `ping` is not optional — the server terminates a connection that fails to pong
     * within its timeout, so a client that ignores pings is dropped mid-session.
     */
    internal data class ControlFrame(val tid: String?, val action: String?, val response: String?)

    var receivedMessage: ((InboxMessage) -> Unit)? = null
    var receivedMessageEvent: ((MessageEvent) -> Unit)? = null

    init {
        onMessageReceived = { data ->
            convertToType(data)
        }
    }

    /**
     * Route an incoming v2 frame.
     *
     * Three kinds arrive on one socket, distinguished without a `type` field (which v2 does not
     * send):
     *
     * - a control frame carrying `action` or `response` — ack, config, or a `ping` that must be
     *   answered
     * - `{"event":"message","data":{…}}` — a new message, canonical shape
     * - anything else with an `event` — a message event (read, archived, …)
     *
     * Ordering matters: control frames are checked first, because a `ping` has no `event` and
     * would otherwise fall through to a failed message decode and surface as a spurious error
     * every heartbeat interval.
     */
    private fun convertToType(data: String) {
        try {
            val control = runCatching { gson.fromJson(data, ControlFrame::class.java) }.getOrNull()
            if (control?.action == "ping") {
                // Mandatory: the server terminates a connection that does not pong in time.
                CoroutineScope(Dispatchers.IO).launch { sendPong(control.tid) }
                return
            }
            // ack / pong / config carry no payload this SDK acts on, but they are not errors.
            if (control?.response != null) {
                return
            }

            val frame = runCatching { gson.fromJson(data, MessageFrame::class.java) }.getOrNull()
            if (frame?.event == "message" && frame.data != null) {
                if (frame.iwpv != null && frame.iwpv != PROTOCOL_VERSION) {
                    // A downgrade would otherwise be invisible until tracking silently stopped.
                    Courier.shared.client?.log(
                        "Inbox socket negotiated ${frame.iwpv} but this client expects $PROTOCOL_VERSION. Tracking ids may be missing."
                    )
                }
                receivedMessage?.invoke(frame.data)
                return
            }

            val messageEvent = gson.fromJson(data, MessageEvent::class.java)
            receivedMessageEvent?.invoke(messageEvent)
        } catch (e: Exception) {
            this.onError?.invoke(e)
        }
    }

    /**
     * Answer a server heartbeat, echoing its `tid` — the server matches the pong against the ping
     * it is waiting on and ignores a mismatched one.
     */
    private suspend fun sendPong(tid: String?) {
        try {
            send(
                mapOf(
                    "tid" to (tid ?: UUID.randomUUID().toString()),
                    "action" to "pong"
                )
            )
        } catch (e: Exception) {
            Courier.shared.client?.log(e.localizedMessage ?: "Failed to answer inbox socket heartbeat")
        }
    }

    /**
     * Subscribe using the IWP envelope: `tid` and `action` at the root, options under `data`.
     *
     * The [version] parameter is retained for source compatibility but is no longer sent — the
     * server hardcodes `clientVersion` for every IWP subscription, so passing it implied a
     * negotiation that does not happen.
     */
    suspend fun sendSubscribe(version: Int = 5) {
        val data = mapOf(
            "tid" to UUID.randomUUID().toString(),
            "action" to "subscribe",
            "data" to mutableMapOf<String, Any>(
                "userAgent" to Courier.agent.value(),
                "event" to "*",
            ).apply {
                options.tenantId?.let { put("accountId", it) }
            }
        )
        send(data)
    }

    companion object {

        /**
         * Inbox wire protocol version, negotiated per connection via the `iwpv` query parameter.
         *
         * `v2` publishes the canonical message — the same object the GraphQL read returns, with
         * `trackingIds` at the root and no nested `data.trackingIds` duplicate.
         *
         * Previously this SDK sent no `iwpv` at all, which the server treats as `legacy`: a
         * different envelope (no `tid`, `keepAlive` instead of ping/pong) and a frame whose message
         * fields sit at the root. That is why `clickTrackingId` had to read `data["trackingIds"]`
         * first — on `legacy` the nested copy is the only one present.
         *
         * The server downgrades an unrecognized version to `legacy` rather than rejecting the
         * connection, so this must not be raised ahead of server support: a client asking for a
         * version the server does not know gets the legacy shape and no error.
         */
        internal const val PROTOCOL_VERSION = "v2"

        /**
         * `cid` and `iwpv` are both required by the IWP handler — it rejects a connection missing
         * either, so they are appended for the client-key path as well as the JWT path.
         */
        internal fun buildUrl(options: CourierClient.Options): String {
            var url = options.apiUrls.inboxWebSocket
            url += when {
                options.jwt != null -> "/?auth=${options.jwt}"
                options.clientKey != null -> "/?clientKey=${options.clientKey}"
                else -> "/?"
            }

            // The server requires a client id; fall back to a generated one so a caller that never
            // set connectionId still gets a valid IWP connection rather than a rejected one.
            val clientId = options.connectionId ?: UUID.randomUUID().toString()

            url += "&cid=$clientId"
            url += "&iwpv=$PROTOCOL_VERSION"
            url += "&userId=${options.userId}"

            return url
        }

    }

}