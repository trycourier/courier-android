package com.courier.android.repositories

import com.courier.android.Courier
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

    private var webSocket: CourierWebsocket? = null

    suspend fun connectWebsocket(clientKey: String, userId: String, onMessageReceived: (InboxMessage) -> Unit, onMessageReceivedError: (Exception) -> Unit) {

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

    suspend fun getAllMessages(clientKey: String, userId: String, paginationLimit: Int = 24, startCursor: String? = null): InboxData {

        val query = """
            query GetMessages(
                ${'$'}params: FilterParamsInput
                ${'$'}limit: Int = $paginationLimit
                ${'$'}after: String ${if (startCursor != null) "= \"$startCursor\"" else ""}
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

        val query = """
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
            .post(query.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierInboxResponse>()
        return res.data?.count ?: 0

    }

}