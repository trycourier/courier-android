package com.courier.android.repositories

import com.courier.android.Courier
import com.google.gson.Gson
import okhttp3.OkHttpClient

internal open class Repository {

    companion object {
        const val baseRest = "https://api.courier.com"
        const val baseGraphQL = "https://api.courier.com/client/q"
        const val inboxGraphQL = "https://fxw3r7gdm9.execute-api.us-east-1.amazonaws.com/production/q"
        const val inboxWebSocket = "wss://1x60p1o3h8.execute-api.us-east-1.amazonaws.com/production"
    }

    val gson = Gson()

    val http = OkHttpClient.Builder().addNetworkInterceptor { chain ->
        chain.proceed(
            chain.request()
                .newBuilder()
                .header("User-Agent", "${Courier.USER_AGENT.value}/${Courier.VERSION}")
                .header("Content-Type", "application/json")
                .build()
        )
    }.build()

}