package com.courier.android.client

import com.courier.android.models.CourierTrackingEvent
import com.courier.android.utils.dispatch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TrackingClient(private val options: CourierClient.Options): CourierApiClient() {

    suspend fun postTrackingUrl(url: String, event: CourierTrackingEvent) {

        val json = JSONObject(
            mapOf("event" to event.value)
        ).toString()

        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody())
            .build()

        return http.newCall(request).dispatch(
            options = options
        )

    }

}