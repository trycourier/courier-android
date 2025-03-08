package com.courier.android.client

import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.models.CourierPreferenceStatus
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.models.CourierUserPreferences
import com.courier.android.models.CourierUserPreferencesTopic
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PreferenceClient(private val options: CourierClient.Options): CourierApiClient() {

    suspend fun getUserPreferences(paginationCursor: String? = null): CourierUserPreferences {

        var url = "${options.apiUrls.rest}/users/${options.userId}/preferences"

        paginationCursor?.let { cursor ->
            url += "?cursor=$cursor"
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${options.jwt}")
            .get()
            .build()

        return http.newCall(request).dispatch(
            options = options
        )

    }

    suspend fun getUserPreferenceTopic(topicId: String): CourierPreferenceTopic {

        val url = "${options.apiUrls.rest}/users/${options.userId}/preferences/$topicId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${options.jwt}")
            .get()
            .build()

        val res = http.newCall(request).dispatch<CourierUserPreferencesTopic>(
            options = options
        )

        return res.topic

    }

    suspend fun putUserPreferenceTopic(topicId: String, status: CourierPreferenceStatus, hasCustomRouting: Boolean, customRouting: List<CourierPreferenceChannel>) {

        val url = "${options.apiUrls.rest}/users/${options.userId}/preferences/$topicId"

        val json = gson.toJson(mapOf(
            "topic" to mapOf(
                "status" to status.value,
                "has_custom_routing" to hasCustomRouting,
                "custom_routing" to customRouting.map { it.value }
            )
        ))

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${options.jwt}")
            .put(json.toRequestBody())
            .build()

        return http.newCall(request).dispatch(
            options = options
        )

    }

}