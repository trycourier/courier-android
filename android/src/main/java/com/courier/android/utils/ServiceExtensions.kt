package com.courier.android.utils

import com.courier.android.Courier
import com.courier.android.models.CourierServerError
import com.courier.android.modules.isDebugging
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


internal fun String.toGraphQuery(): String {
    val value = this
        .replace("\n", "")
        .replace("\r", "")
        .trim()
    return "{\"query\": \"${value}\"}"
}

internal fun Request.toPrettyJsonBody(): String? {

    if (body == null) {
        return null
    }

    return try {
        val buffer = Buffer()
        body?.writeTo(buffer)
        val body = buffer.readUtf8()
        if (body.isEmpty()) return null
        val gson = GsonBuilder().setLenient().setPrettyPrinting().create()
        val jsonObject = JsonParser.parseString(body).asJsonObject
        return "\n${gson.toJson(jsonObject)}"
    } catch (e: Exception) {
        null
    }

}

internal suspend inline fun <reified T>Call.dispatch(validCodes: List<Int> = listOf(200)) = suspendCoroutine<T> { continuation ->

    if (Courier.shared.isDebugging) {
        val request = request()
        Courier.log("📡 New Courier API Request")
        Courier.log("URL: ${request.url}")
        Courier.log("Method: ${request.method}")
        Courier.log("Body: ${request.toPrettyJsonBody() ?: "Empty"}")
    }

    enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {

            val gson = Gson()

            if (!validCodes.contains(response.code)) {

                try {
                    val error = gson.fromJson(response.body?.string(), CourierServerError::class.java).toException
                    continuation.resumeWithException(error)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }

            } else {

                try {

                    if (T::class == Any::class) {
                        continuation.resume(T::class.java.newInstance())
                        return
                    }

                    val res = gson.fromJson(response.body?.string(), T::class.java)
                    continuation.resume(res)

                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }

            }

        }

    })


}