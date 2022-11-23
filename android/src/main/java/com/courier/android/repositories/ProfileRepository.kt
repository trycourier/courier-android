package com.courier.android.repositories

import com.courier.android.Courier
import com.courier.android.models.*
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ProfileRepository : Repository() {

    suspend fun patchUser(userId: String) = suspendCoroutine { continuation ->

        val accessToken = Courier.shared.accessToken
        if (accessToken == null) {
            continuation.resumeWithException(CourierException.missingAccessToken)
            return@suspendCoroutine
        }

        val url = "$baseUrl/profiles/$userId"

        val json = gson.toJson(
            CourierProfile(
                patch = listOf(ProfilePatchPayload(value = userId))
            )
        )

        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json").patch(json.toRequestBody()).build()


        http.newCall(request).dispatch<Any>(validCodes = listOf(200),
            onSuccess = { continuation.resume(Unit) },
            onFailure = { continuation.resumeWithException(it) })
    }
}