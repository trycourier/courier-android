package com.courier.android.client

import com.courier.android.Courier
import com.google.gson.Gson
import okhttp3.OkHttpClient

open class CourierApiClient {

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