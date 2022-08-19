package com.courier.android.repositories

import com.courier.android.Courier
import com.courier.android.log
import com.courier.android.models.CourierException
import com.courier.android.models.CourierMessageResponse
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierPushEvent
import com.google.gson.Gson
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


internal class MessagingRepository : Repository() {

    internal suspend fun send(authKey: String, userId: String, title: String, body: String, providers: List<CourierProvider>, isProduction: Boolean) = suspendCoroutine { continuation ->

        Courier.log("Sending Courier Message")

        val url = "$baseUrl/send"

        val json = JSONObject(
            mapOf(
                "message" to mapOf(
                    "to" to mapOf(
                        "user_id" to userId
                    ),
                    "content" to mapOf(
                        "title" to title,
                        "body" to body
                    ),
                    "routing" to mapOf(
                        "method" to "all",
                        "channels" to providers.map { it.value }
                    ),
                    "providers" to mapOf(
                        "firebase-fcm" to mapOf(
                            "override" to mapOf(
                                "body" to mapOf(
                                    "notification" to null,
                                    "data" to mapOf(
                                        "title" to title,
                                        "body" to body
                                    )
                                )
                            )
                        ),
                        "apn" to mapOf(
                            "override" to mapOf(
                                "config" to mapOf(
                                    "isProduction" to isProduction
                                ),
                                "body" to mapOf(
                                    "mutableContent" to 1
                                )
                            )
                        )
                    )
                )
            )
        ).toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody())
            .build()

        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!listOf(202, 202).contains(response.code)) {
                    continuation.resumeWithException(CourierException.requestError)
                } else {
                    try {
                        val messageResponse = gson.fromJson(response.body?.string(), CourierMessageResponse::class.java)
                        val requestId = messageResponse.requestId
                        Courier.log("New Courier message sent. View logs here:")
                        Courier.log("https://app.courier.com/logs/messages?message=${requestId}")
                        continuation.resume(requestId)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })

    }

    internal suspend fun postTrackingUrl(url: String, event: CourierPushEvent) = suspendCoroutine { continuation ->

        val json = JSONObject(
            mapOf("event" to event.value)
        ).toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody())
            .build()

        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!listOf(200).contains(response.code)) {
                    continuation.resumeWithException(CourierException.requestError)
                } else {
                    continuation.resume(Unit)
                }
            }
        })

    }

}