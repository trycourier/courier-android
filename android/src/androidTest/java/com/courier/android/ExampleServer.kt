package com.courier.android

import com.courier.android.repositories.Repository
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class ExampleServer : Repository() {

    data class TokenResponse(val token: String)
    data class MessageResponse(val requestId: String)

    internal suspend fun generateJWT(authKey: String, userId: String): String {

        val json = JSONObject(
            mapOf(
                "scope" to "user_id:$userId write:user-tokens inbox:read:messages inbox:write:events read:preferences write:preferences read:brands",
                "expires_in" to "2 days"
            )
        ).toString()

        val request = Request.Builder()
            .url("https://api.courier.com/auth/issue-token")
            .addHeader("Authorization", "Bearer $authKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<TokenResponse>()
        return res.token

    }

    internal suspend fun sendTest(authKey: String, userId: String, channel: String): String {

        val json = JSONObject(
            mapOf(
                "message" to mapOf(
                    "to" to mapOf(
                        "user_id" to userId
                    ),
                    "content" to mapOf(
                        "title" to "Test",
                        "body" to "Body"
                    ),
                    "routing" to mapOf(
                        "method" to "single",
                        "channels" to listOf(channel)
                    )
                )
            )
        ).toString()

        val request = Request.Builder()
            .url("https://api.courier.com/send")
            .addHeader("Authorization", "Bearer $authKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<MessageResponse>(validCodes = listOf(202))
        return res.requestId

    }

}