package com.courier.android.client

import com.courier.android.models.CourierGetInboxMessageResponse
import com.courier.android.models.CourierGetInboxMessagesResponse
import com.courier.android.socket.NewInboxSocket
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class InboxClient(private val options: CourierClient.Options): CourierApiClient() {

    val socket by lazy { NewInboxSocket(options = options) }

    suspend fun getMessage(messageId: String): CourierGetInboxMessageResponse {

        // TODO: Support tenants
        val tenantParams = if (options.tenantId != null) """accountId: \"${options.tenantId}\"""" else ""

        val query = """
            query GetInboxMessage {
                message(messageId: \"$messageId\") {
                    messageId
                    read
                    archived
                    created
                    opened
                    data
                    trackingIds {
                        clickTrackingId
                    }
                    content {
                        title
                        preview
                        actions {
                            background_color
                            content
                            href
                            style
                        }
                    }
                }
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        return http.newCall(request).dispatch<CourierGetInboxMessageResponse>(
            options = options
        )

    }

    suspend fun getMessages(paginationLimit: Int = 24, startCursor: String? = null): CourierGetInboxMessagesResponse {

        val tenantParams = if (options.tenantId != null) """accountId: \"${options.tenantId}\"""" else ""

        val query = """
            query GetInboxMessages(
                ${'$'}params: FilterParamsInput = { $tenantParams }
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
                            clickTrackingId
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
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        return http.newCall(request).dispatch<CourierGetInboxMessagesResponse>(
            options = options
        )

    }

    suspend fun getUnreadMessageCount(): Int {

        val tenantParams = if (options.tenantId != null) """, accountId: \"${options.tenantId}\"""" else ""

        val mutation = """
            query GetMessages {
                count(params: { status: \"unread\" $tenantParams })
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierGetInboxMessagesResponse>(
            options = options
        )

        return res.data?.count ?: 0

    }

    suspend fun trackClick(messageId: String, trackingId: String) {

        val mutation = """
            mutation TrackEvent {
                clicked(messageId: \"${messageId}\", trackingId: \"${trackingId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.connectionId?.let { addHeader("x-courier-client-source-id", options.connectionId) }
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options
        )

    }

    suspend fun trackRead(messageId: String) {

        val mutation = """
            mutation TrackEvent {
                read(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.connectionId?.let { addHeader("x-courier-client-source-id", options.connectionId) }
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options
        )

    }

    suspend fun trackUnread(messageId: String) {

        val query = """
            mutation TrackEvent {
                unread(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.connectionId?.let { addHeader("x-courier-client-source-id", options.connectionId) }
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options
        )

    }

    suspend fun trackAllRead() {

        val query = """
            mutation TrackEvent {
                markAllRead
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.connectionId?.let { addHeader("x-courier-client-source-id", options.connectionId) }
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options
        )

    }

    suspend fun trackOpened(messageId: String) {

        val query = """
            mutation TrackEvent {
                opened(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.connectionId?.let { addHeader("x-courier-client-source-id", options.connectionId) }
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options
        )

    }

    suspend fun trackArchive(messageId: String) {

        val mutation = """
            mutation TrackEvent {
                archive(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", options.userId)
            .apply {
                options.connectionId?.let { addHeader("x-courier-client-source-id", options.connectionId) }
                options.jwt?.let { addHeader("Authorization", "Bearer $it") } ?: options.clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options
        )

    }

}