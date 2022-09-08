package com.courier.android.utils

import com.courier.android.models.CourierServerError
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

internal inline fun <reified T>Call.dispatch(validCodes: List<Int> = listOf(200), crossinline onSuccess: (T) -> Unit, crossinline onFailure: (Exception) -> Unit) {

    enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            onFailure(e)
        }

        override fun onResponse(call: Call, response: Response) {

            val gson = Gson()

            if (!validCodes.contains(response.code)) {

                try {
                    val error = gson.fromJson(response.body?.string(), CourierServerError::class.java)
                    onFailure(error.toException)
                } catch (e: Exception) {
                    onFailure(e)
                }

            } else {

                try {

                    if (T::class == Unit::class) {
                        onSuccess(T::class.java.newInstance())
                        return
                    }

                    val res = gson.fromJson(response.body?.string(), T::class.java)
                    onSuccess(res)

                } catch (e: Exception) {
                    onFailure(e)
                }

            }

        }

    })


}