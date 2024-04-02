package com.courier.example

import com.google.gson.Gson
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class ExampleServer {

    data class Response(val token: String)

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

        val res = OkHttpClient().newCall(request).dispatch<Response>()
        return res.token

    }

    private suspend inline fun <reified T> Call.dispatch() = suspendCoroutine<T> { continuation ->

        enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {

                val gson = Gson()

                val body = response.body?.string()

                try {

                    if (T::class == Any::class) {
                        continuation.resume(T::class.java.newInstance())
                        return
                    }

                    val res = gson.fromJson(body, T::class.java)
                    continuation.resume(res)

                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }

            }

        })


    }

}