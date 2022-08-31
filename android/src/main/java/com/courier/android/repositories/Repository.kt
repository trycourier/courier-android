package com.courier.android.repositories

import com.courier.android.Courier
import com.google.gson.Gson
import okhttp3.OkHttpClient

sealed class Repository {

    val baseUrl = "https://api.courier.com"
    val gson = Gson()

    val http
        get() = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            chain.proceed(
                chain.request()
                    .newBuilder()
                    .header("User-Agent", "${Courier.AGENT.value}/${Courier.VERSION}")
                    .build()
            )
        }.build()

}