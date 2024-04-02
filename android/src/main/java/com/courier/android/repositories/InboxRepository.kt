package com.courier.android.repositories

import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxResponse
import com.courier.android.models.InboxData
import com.courier.android.models.InboxMessage
import com.courier.android.socket.CourierInboxWebsocket
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import com.google.gson.Gson
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class InboxRepository : Repository() {

    internal fun connectWebsocket(clientKey: String? = null, userId: String, onMessageReceived: (InboxMessage) -> Unit, onMessageReceivedError: (Exception) -> Unit) {

        if (CourierInboxWebsocket.shared?.isSocketConnected == true || CourierInboxWebsocket.shared?.isSocketConnecting == true) {
            return
        }

        CourierInboxWebsocket.onMessageReceived = { message ->
            try {
                val inboxMessage = Gson().fromJson(message, InboxMessage::class.java)
                onMessageReceived(inboxMessage)
            } catch (e: Exception) {
                onMessageReceivedError(e)
            }
        }

        CourierInboxWebsocket.connect(
            userId = userId,
            clientKey = clientKey
        )

    }

    fun disconnectWebsocket() {
        CourierInboxWebsocket.disconnect()
    }

    internal suspend fun getMessages(clientKey: String? = null, jwt: String? = null, userId: String, paginationLimit: Int = 24, startCursor: String? = null): InboxData {

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
                        trackingIds {
                            openTrackingId
                            archiveTrackingId
                            clickTrackingId
                            deliverTrackingId
                            readTrackingId
                            unreadTrackingId
                        }
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
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierInboxResponse>()
        res.data?.let { return it }
        throw CourierException.jsonParsingError

    }

    internal suspend fun getUnreadMessageCount(clientKey: String? = null, jwt: String? = null, userId: String): Int {

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
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierInboxResponse>()
        return res.data?.count ?: 0

    }

    internal suspend fun clickMessage(clientKey: String? = null, jwt: String? = null, userId: String, messageId: String, channelId: String) {

        val mutation = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
                ${'$'}trackingId: String = \"${channelId}\"
            ) {
                clicked(messageId: ${'$'}messageId, trackingId: ${'$'}trackingId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun readMessage(clientKey: String? = null, jwt: String? = null, userId: String, messageId: String) {

        val mutation = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
            ) {
                read(messageId: ${'$'}messageId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun unreadMessage(clientKey: String? = null, jwt: String? = null, userId: String, messageId: String) {

        val query = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
            ) {
                unread(messageId: ${'$'}messageId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun readAllMessages(clientKey: String? = null, jwt: String? = null, userId: String, ) {

        val query = """
            mutation TrackEvent {
                markAllRead
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun openMessage(clientKey: String? = null, jwt: String? = null, userId: String, messageId: String) {

        val query = """
            mutation TrackEvent(
                ${'$'}messageId: String = \"${messageId}\"
            ) {
                opened(messageId: ${'$'}messageId)
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(inboxGraphQL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

}