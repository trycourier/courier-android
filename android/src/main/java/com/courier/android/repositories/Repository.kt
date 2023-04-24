package com.courier.android.repositories

import com.courier.android.Courier
import com.google.gson.Gson
import okhttp3.OkHttpClient

sealed class Repository {

    val gson = Gson()
    val baseRest = "https://api.courier.com"
    val baseGraphQL = "https://api.courier.com/client/q"
    val inboxGraphQL = "https://fxw3r7gdm9.execute-api.us-east-1.amazonaws.com/production/q"
    val inboxWebSocket = "wss://1x60p1o3h8.execute-api.us-east-1.amazonaws.com/production"

    val http
        get() = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            chain.proceed(
                chain.request()
                    .newBuilder()
                    .header("User-Agent", "${Courier.USER_AGENT.value}/${Courier.VERSION}")
                    .header("Content-Type", "application/json")
                    .build()
            )
        }.build()

}