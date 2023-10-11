package com.courier.android

import com.courier.android.repositories.Repository
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class ExampleServer : Repository() {

    data class Response(val token: String)

    internal suspend fun generateJWT(authKey: String, userId: String): String {

        val json = JSONObject(
            mapOf(
                "scope" to "user_id:$userId write:user-tokens write:preferences read:preferences",
                "expires_in" to "2 days"
            )
        ).toString()

        val request = Request.Builder()
            .url("https://api.courier.com/auth/issue-token")
            .addHeader("Authorization", "Bearer $authKey")
            .post(json.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<Response>()
        return res.token

    }

}