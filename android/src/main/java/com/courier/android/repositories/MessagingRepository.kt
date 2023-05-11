package com.courier.android.repositories

import com.courier.android.Courier
import com.courier.android.models.CourierMessageResponse
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierPushEvent
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


internal class MessagingRepository : Repository() {

    internal suspend fun send(authKey: String, userIds: List<String>, title: String, body: String, providers: List<CourierProvider>): String {

        // TODO: Update channels

        val url = "$baseRest/send"

        val json = JSONObject(
            mapOf(
                "message" to mapOf(
                    "to" to userIds.map {
                        mapOf("user_id" to it)
                    },
                    "content" to mapOf(
                        "title" to title,
                        "body" to body
                    ),
                    "routing" to mapOf(
                        "method" to "all",
                        "channels" to providers.map { it.value }
                    ),
                )
            )
        ).toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $authKey")
            .post(json.toRequestBody())
            .build()

        val res = http.newCall(request).dispatch<CourierMessageResponse>(validCodes = listOf(200, 202))

        val requestId = res.requestId

        Courier.log("New Courier message sent. View logs here:\n" +
                "https://app.courier.com/logs/messages?message=${requestId}\n" +
                "If you do not receive this message, you may need to configure the Firebase Cloud Messaging provider. More info:\n" +
                "https://app.courier.com/channels/firebase-fcm")

        return requestId

    }

    internal suspend fun postTrackingUrl(url: String, event: CourierPushEvent) {

        val json = JSONObject(
            mapOf("event" to event.value)
        ).toString()

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody())
            .build()

        http.newCall(request).dispatch<CourierMessageResponse>()

    }

}