package com.courier.android.repositories

import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxResponse
import com.courier.android.models.InboxData
import com.courier.android.models.InboxMessage
import com.courier.android.socket.CourierWebsocket
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import com.google.gson.Gson
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class InboxRepository : Repository() {

    internal var webSocket: CourierWebsocket? = null

    internal suspend fun connectWebsocket(clientKey: String, userId: String, onMessageReceived: (InboxMessage) -> Unit, onMessageReceivedError: (Exception) -> Unit) {

        if (webSocket?.isSocketConnected == true) {
            return
        }

        webSocket = CourierWebsocket(
            url = "$inboxWebSocket/?clientKey=$clientKey",
            onMessageReceived = { message ->
                try {
                    val inboxMessage = Gson().fromJson(message, InboxMessage::class.java)
                    onMessageReceived(inboxMessage)
                } catch (e: Exception) {
                    onMessageReceivedError(e)
                }
            },
        )

        val json = """
            {
                "action": "subscribe",
                "data": {
                    "channel": "$userId",
                    "clientKey": "$clientKey",
                    "event": "*",
                    "version": "4"
                }
            }
        """

        webSocket?.connect(json)

    }

    suspend fun disconnectWebsocket() {
        webSocket?.disconnect()
    }

    internal suspend fun getMessages(clientKey: String, userId: String, paginationLimit: Int = 24, startCursor: String? = null): InboxData {

        val query = """
            query GetMessages(
                ${'$'}params: FilterParamsInput
                ${'$'}limit: Int = $paginationLimit
                ${'$'}after: String ${if (startCursor != null) "= \\\"${startCursor}\\\"" else ""}
            ) {
                count(params: ${'$'}params)
                messages(params: ${'$'}params, limit: ${'$'}limit, after: ${'$'}after) {
                    totalCount
                    pageInfo {
                        startCursor
                        hasNextPage
                    }
                    nodes {
                        messageId
                        read
                        archived
                        created
                        opened
                        title
                        preview
                        data
                        actions {
                            content
                            data
                            href
                        }
                    }
                }
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-client-key", clientKey)
            .addHeader("x-courier-user-id", userId)
            .post(query.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierInboxResponse>()
        res.data?.let { return it }
        throw CourierException.jsonParsingError

    }

    internal suspend fun getUnreadMessageCount(clientKey: String, userId: String): Int {

        val mutation = """
            query GetMessages(
                ${'$'}params: FilterParamsInput = { status: \"unread\" }
                ${'$'}limit: Int = ${1}
                ${'$'}after: String
            ) {
                count(params: ${'$'}params)
                messages(params: ${'$'}params, limit: ${'$'}limit, after: ${'$'}after) {
                    nodes {
                        messageId
                    }
                }
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-client-key", clientKey)
            .addHeader("x-courier-user-id", userId)
            .post(mutation.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierInboxResponse>()
        return res.data?.count ?: 0

    }

    internal suspend fun readMessage(clientKey: String, userId: String, messageId: String) {

        val mutation = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
            ) {
                read(messageId: ${'$'}messageId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-client-key", clientKey)
            .addHeader("x-courier-user-id", userId)
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun unreadMessage(clientKey: String, userId: String, messageId: String) {

        val query = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
            ) {
                unread(messageId: ${'$'}messageId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-client-key", clientKey)
            .addHeader("x-courier-user-id", userId)
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun readAllMessages(clientKey: String, userId: String) {

        val query = """
            mutation TrackEvent {
                markAllRead
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-client-key", clientKey)
            .addHeader("x-courier-user-id", userId)
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun openMessage(clientKey: String, userId: String, messageId: String) {

        val query = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
            ) {
                opened(messageId: ${'$'}messageId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-client-key", clientKey)
            .addHeader("x-courier-user-id", userId)
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

}