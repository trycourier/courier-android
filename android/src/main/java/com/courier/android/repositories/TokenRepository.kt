package com.courier.android.repositories

import com.courier.android.Courier
import com.courier.android.log
import com.courier.android.models.CourierDevice
import com.courier.android.models.CourierException
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierToken
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class TokenRepository : Repository() {

    suspend fun putUserToken(messagingToken: String, provider: CourierProvider) = suspendCoroutine { continuation ->

        Courier.log("Putting Messaging Token")

        val accessToken = Courier.shared.accessToken
        if (accessToken == null) {
            continuation.resumeWithException(CourierException.missingAccessToken)
            return@suspendCoroutine
        }

        val userId = Courier.shared.userId
        if (userId == null) {
            continuation.resumeWithException(CourierException.missingUserId)
            return@suspendCoroutine
        }

        val url = "$baseUrl/users/$userId/tokens/$messagingToken"

        val json = gson.toJson(CourierToken(
            provider_key = provider.value,
            device = CourierDevice.current
        ))

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .put(json.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            validCodes = listOf(202, 204),
            onSuccess = { continuation.resume(Unit) },
            onFailure = { continuation.resumeWithException(it) }
        )

    }

    suspend fun deleteUserToken(messagingToken: String) = suspendCoroutine { continuation ->

        Courier.log("Deleting Messaging Token")

        val accessToken = Courier.shared.accessToken
        if (accessToken == null) {
            continuation.resumeWithException(CourierException.missingAccessToken)
            return@suspendCoroutine
        }

        val userId = Courier.shared.userId
        if (userId == null) {
            continuation.resumeWithException(CourierException.missingUserId)
            return@suspendCoroutine
        }

        val url = "$baseUrl/users/$userId/tokens/$messagingToken"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .delete()
            .build()

        http.newCall(request).dispatch<Any>(
            validCodes = listOf(202, 204),
            onSuccess = { continuation.resume(Unit) },
            onFailure = { continuation.resumeWithException(it) }
        )

    }

}