package com.courier.android.client

import com.courier.android.models.CourierDevice
import com.courier.android.models.CourierToken
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TokenClient(private val options: CourierClient.Options) : CourierApiClient() {

    suspend fun putUserToken(token: String, provider: String, device: CourierDevice = CourierDevice.current) {

        val url = "${BASE_REST}/users/${options.userId}/tokens/$token"

        val json = gson.toJson(
            CourierToken(
                provider_key = provider,
                device = device
            )
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${options.jwt}")
            .put(json.toRequestBody())
            .build()

        http.newCall(request).dispatch<Any>(
            options = options,
            validCodes = listOf(202, 204),
        )

    }

    suspend fun deleteUserToken(token: String) {

        val url = "${BASE_REST}/users/${options.userId}/tokens/$token"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${options.jwt}")
            .delete()
            .build()

        http.newCall(request).dispatch<Any>(
            options = options,
            validCodes = listOf(202, 204),
        )

    }

}