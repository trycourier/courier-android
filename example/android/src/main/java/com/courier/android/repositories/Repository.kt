package com.courier.android.repositories

import com.google.gson.Gson
import okhttp3.OkHttpClient

sealed class Repository {

    val http = OkHttpClient()
    val baseUrl = "https://api.courier.com"
    val gson = Gson()
//    val baseUrl = "https://9ja2q3sgqi.execute-api.us-east-1.amazonaws.com/dev"

}