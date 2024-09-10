package com.courier.android.client

import com.courier.android.Courier
import com.google.gson.Gson
import okhttp3.OkHttpClient

open class CourierApiClient {

    companion object {
        const val BASE_REST = "https://api.courier.com"
        const val BASE_GRAPH_QL = "https://api.courier.com/client/q"
        const val INBOX_GRAPH_QL = "https://fxw3r7gdm9.execute-api.us-east-1.amazonaws.com/production/q"
        const val INBOX_WEBSOCKET = "wss://1x60p1o3h8.execute-api.us-east-1.amazonaws.com/production"
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