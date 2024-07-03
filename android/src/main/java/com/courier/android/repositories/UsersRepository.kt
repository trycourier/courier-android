package com.courier.android.repositories

import com.courier.android.models.*
import com.courier.android.models.CourierDevice
import com.courier.android.models.CourierToken
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class UsersRepository : Repository() {

    suspend fun putUserToken(accessToken: String, userId: String, token: String, provider: String) {

        val url = "$BASE_REST/users/$userId/tokens/$token"

        val json = gson.toJson(CourierToken(
            provider_key = provider,
            device = CourierDevice.current
        ))

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .put(json.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(validCodes = listOf(202, 204))

    }

    suspend fun deleteUserToken(accessToken: String, userId: String, token: String) {

        val url = "$BASE_REST/users/$userId/tokens/$token"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        http.newCall(request).dispatch<Any>(validCodes = listOf(202, 204))

    }

    suspend fun getUserPreferences(accessToken: String, userId: String, paginationCursor: String? = null): CourierUserPreferences {

        var url = "$BASE_REST/users/$userId/preferences"

        paginationCursor?.let { cursor ->
            url += "?cursor=$cursor"
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return http.newCall(request).dispatch()

    }

    suspend fun getUserPreferenceTopic(accessToken: String, userId: String, topicId: String): CourierPreferenceTopic {

        val url = "$BASE_REST/users/$userId/preferences/$topicId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val res = http.newCall(request).dispatch<CourierUserPreferencesTopic>()
        return res.topic

    }

    suspend fun putUserPreferenceTopic(accessToken: String, userId: String, topicId: String, status: CourierPreferenceStatus, hasCustomRouting: Boolean, customRouting: List<CourierPreferenceChannel>) {

        val url = "$BASE_REST/users/$userId/preferences/$topicId"

        val json = gson.toJson(mapOf(
            "topic" to mapOf(
                "status" to status.value,
                "has_custom_routing" to hasCustomRouting,
                "custom_routing" to customRouting.map { it.value }
            )
        ))

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .put(json.toRequestBody())
            .build()

        return http.newCall(request).dispatch()

    }

}