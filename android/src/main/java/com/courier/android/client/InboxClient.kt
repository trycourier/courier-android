package com.courier.android.client

import com.courier.android.models.CourierInboxResponse
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class InboxClient(private val options: CourierClient.Options): CourierApiClient() {

    suspend fun getMessages(paginationLimit: Int = 24, startCursor: String? = null): CourierInboxResponse {

        val tenantParams = if (options.tenantId != null) """accountId: \"${options.tenantId}\"""" else ""

        val query = """
            query GetMessages(
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

        return http.newCall(request).dispatch<CourierInboxResponse>()

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

        val res = http.newCall(request).dispatch<CourierInboxResponse>()
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

        http.newCall(request).dispatch<Any>()

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

        http.newCall(request).dispatch<Any>()

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

        http.newCall(request).dispatch<Any>()

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

        http.newCall(request).dispatch<Any>()

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

        http.newCall(request).dispatch<Any>()

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

        http.newCall(request).dispatch<Any>()

    }

}