package com.courier.android.utils

import com.courier.android.models.CourierServerError
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal fun String.toGraphQuery(): String = "{\"query\": \"${this.trimIndent().replace("\n", "")}\"}"

internal suspend inline fun <reified T>Call.dispatch(validCodes: List<Int> = listOf(200)) = suspendCoroutine<T> { continuation ->

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