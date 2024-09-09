package com.courier.android

import com.courier.android.client.CourierClient
import com.courier.android.utils.dispatch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class ExampleServer {

    data class TokenResponse(val token: String)
    data class MessageResponse(val requestId: String)

    companion object {

        private val mockOptions = CourierClient.Options(
            null,
            null,
            Env.COURIER_USER_ID,
            null,
            null,
            true,
        )

        private val http = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            chain.proceed(
                chain.request()
                    .newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
            )
        }.build()

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

            val res = http.newCall(request).dispatch<TokenResponse>(mockOptions)
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

            val res = http.newCall(request).dispatch<MessageResponse>(
                options = mockOptions,
                validCodes = listOf(202)
            )

            return res.requestId

        }

    }

}