package com.courier.android.repositories

import com.courier.android.models.CourierDevice
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierToken
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class TokenRepository : Repository() {

    suspend fun putUserToken(accessToken: String, userId: String, token: String, provider: CourierProvider) {

        val url = "$baseRest/users/$userId/tokens/$token"

        val json = gson.toJson(CourierToken(
            provider_key = provider.value,
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

        val url = "$baseRest/users/$userId/tokens/$token"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        http.newCall(request).dispatch<Any>(validCodes = listOf(202, 204))

    }

}