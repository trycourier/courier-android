package com.courier.android.repositories

import com.courier.android.Courier
import com.courier.android.models.*
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import org.json.JSONObject

class UserRepository : Repository() {

    suspend fun patchUser(userId: String) = suspendCoroutine { continuation ->

        val accessToken = Courier.shared.accessToken
        if (accessToken == null) {
            continuation.resumeWithException(CourierException.missingAccessToken)
            return@suspendCoroutine
        }

        val url = "$baseUrl/profiles/$userId"

        val json = JSONObject(
            mapOf(
                "patch" to listOf(
                    mapOf(
                        "op" to "replace",
                        "path" to "/user_id",
                        "value" to userId
                    )
                )
            )
        ).toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .patch(json.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            onSuccess = { continuation.resume(Unit) },
            onFailure = { continuation.resumeWithException(it) }
        )

    }

}