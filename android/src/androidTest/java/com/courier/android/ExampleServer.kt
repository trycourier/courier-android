package com.courier.android

import com.courier.android.client.CourierClient
import com.courier.android.utils.dispatch
import com.courier.android.utils.urlPathSegment
import com.courier.app.Env
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class ExampleServer {

    data class TokenResponse(val token: String)
    data class MessageResponse(val requestId: String)

    companion object {

        private val mockOptions = CourierClient.Options(
            jwt = null,
            clientKey = null,
            userId = Env.COURIER_USER_ID,
            connectionId = null,
            tenantId = null,
            apiUrls = CourierClient.ApiUrls(),
            showLogs = true,
        )

        private val http = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            chain.proceed(
                chain.request()
                    .newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
            )
        }.build()

        internal suspend fun generateJWT(authKey: String, userId: String, expiresIn: String = "2 days"): String {

            val json = JSONObject(
                mapOf(
                    "scope" to "user_id:$userId write:user-tokens inbox:read:messages inbox:write:events read:preferences write:preferences read:brands",
                    "expires_in" to expiresIn
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

        internal suspend fun sendTest(authKey: String, userId: String, tenantId: String? = null, channel: String): String {

            val toField: Map<String, String> = if (tenantId != null) {
                mapOf("tenant_id" to tenantId)
            } else {
                mapOf("user_id" to userId)
            }

            val json = JSONObject(
                mapOf(
                    "message" to mapOf(
                        "to" to toField,
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

        // Every token the server stores for the user, exactly as stored
        internal suspend fun userTokens(authKey: String, userId: String): List<String> {

            val request = Request.Builder()
                .url("https://api.courier.com/users/${userId.urlPathSegment}/tokens")
                .addHeader("Authorization", "Bearer $authKey")
                .get()
                .build()

            return withContext(Dispatchers.IO) {
                http.newCall(request).execute().use { response ->
                    if (response.code == 404) return@use emptyList()
                    val tokens = JSONObject(response.body?.string() ?: "{}").optJSONArray("tokens")
                    (0 until (tokens?.length() ?: 0)).map { tokens!!.getJSONObject(it).getString("token") }
                }
            }

        }

        // Whether the server has the token for the user, checked with the API key
        internal suspend fun hasUserToken(authKey: String, userId: String, token: String): Boolean {

            val request = Request.Builder()
                .url("https://api.courier.com/users/${userId.urlPathSegment}/tokens/${token.urlPathSegment}")
                .addHeader("Authorization", "Bearer $authKey")
                .get()
                .build()

            return withContext(Dispatchers.IO) {
                http.newCall(request).execute().use { it.code == 200 }
            }

        }

    }

}