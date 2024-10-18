package com.courier.android.client

import com.courier.android.Courier
import com.google.gson.Gson
import okhttp3.OkHttpClient

open class CourierApiClient {

    companion object {
        const val BASE_REST = "https://api.courier.com"
        const val BASE_GRAPH_QL = "https://api.courier.com/client/q"
        const val INBOX_GRAPH_QL = "https://inbox.courier.com/q"
        const val INBOX_WEBSOCKET = "wss://realtime.courier.com"
    }

    val gson = Gson()

    val http = OkHttpClient.Builder().addNetworkInterceptor { chain ->
        chain.proceed(
            chain.request()
                .newBuilder()
                .header("User-Agent", Courier.agent.value())
                .header("Content-Type", "application/json")
                .build()
        )
    }.build()

}