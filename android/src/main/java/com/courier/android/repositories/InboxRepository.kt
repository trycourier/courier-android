package com.courier.android.repositories

import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxResponse
import com.courier.android.models.InboxData
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class InboxRepository : Repository() {

    internal suspend fun getMessages(clientKey: String? = null, jwt: String? = null, userId: String, tenantId: String? = null, paginationLimit: Int = 24, startCursor: String? = null): InboxData {

        val tenantParams = if (tenantId != null) """accountId: \"${tenantId}\"""" else ""

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

    internal suspend fun getUnreadMessageCount(clientKey: String? = null, jwt: String? = null, userId: String, tenantId: String? = null): Int {

        val tenantParams = if (tenantId != null) """, accountId: \"${tenantId}\"""" else ""

        val mutation = """
            query GetMessages {
                count(params: { status: \"unread\" $tenantParams })
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
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

    internal suspend fun trackClick(clientKey: String? = null, jwt: String? = null, userId: String, messageId: String, trackingId: String) {

        val mutation = """
            mutation TrackEvent {
                clicked(messageId: \"${messageId}\", trackingId: \"${trackingId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", userId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun trackRead(clientKey: String? = null, jwt: String? = null, userId: String, connectionId: String, messageId: String) {

        val mutation = """
            mutation TrackEvent {
                read(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", userId)
            .addHeader("x-courier-client-source-id", connectionId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun trackUnread(clientKey: String? = null, jwt: String? = null, userId: String, connectionId: String, messageId: String) {

        val query = """
            mutation TrackEvent {
                unread(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", userId)
            .addHeader("x-courier-client-source-id", connectionId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun trackAllRead(clientKey: String? = null, jwt: String? = null, connectionId: String, userId: String, ) {

        val query = """
            mutation TrackEvent {
                markAllRead
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", userId)
            .addHeader("x-courier-client-source-id", connectionId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun trackOpened(clientKey: String? = null, jwt: String? = null, userId: String, connectionId: String, messageId: String) {

        val query = """
            mutation TrackEvent {
                opened(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", userId)
            .addHeader("x-courier-client-source-id", connectionId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(query.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

    internal suspend fun trackArchive(clientKey: String? = null, jwt: String? = null, userId: String, connectionId: String, messageId: String) {

        val mutation = """
            mutation TrackEvent {
                archive(messageId: \"${messageId}\")
            }
        """.toGraphQuery()

        val request = Request.Builder()
            .url(INBOX_GRAPH_QL)
            .addHeader("x-courier-user-id", userId)
            .addHeader("x-courier-client-source-id", connectionId)
            .apply {
                jwt?.let { addHeader("Authorization", "Bearer $it") }
                    ?: clientKey?.let { addHeader("x-courier-client-key", it) }
            }
            .post(mutation.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>()

    }

}