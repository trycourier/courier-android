package com.courier.android.repositories

import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxResponse
import com.courier.android.models.InboxData
import com.courier.android.utils.dispatch
import com.courier.android.utils.toGraphQuery
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class InboxRepository : Repository() {

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
                ${'$'}params: FilterParamsInput = { status: "unread" }
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