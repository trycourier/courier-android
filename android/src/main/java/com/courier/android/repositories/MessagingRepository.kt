package com.courier.android.repositories

import com.courier.android.Courier
import com.courier.android.log
import com.courier.android.models.CourierMessageResponse
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierPushEvent
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
                                    "data" to CourierProvider.FCM.generatePayload(title, body),
                                    "apns" to CourierProvider.APNS.generatePayload(title, body)
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

        http.newCall(request).dispatch<CourierMessageResponse>(
            validCodes = listOf(200, 202),
            onSuccess = {  res ->
                val requestId = res.requestId
                Courier.log("New Courier message sent. View logs here:\n" +
                        "https://app.courier.com/logs/messages?message=${requestId}\n" +
                        "If you do not receive this message, you may need to configure the Firebase Cloud Messaging provider. More info:\n" +
                        "https://app.courier.com/channels/firebase-fcm")
                continuation.resume(requestId)
            },
            onFailure = {
                continuation.resumeWithException(it)
            }
        )

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

        http.newCall(request).dispatch<CourierMessageResponse>(
            validCodes = listOf(200),
            onSuccess = { continuation.resume(it) },
            onFailure = { continuation.resumeWithException(it) }
        )

    }

}