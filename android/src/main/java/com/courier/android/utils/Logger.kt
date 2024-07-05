package com.courier.android.utils

import android.util.Log
import com.courier.android.client.CourierClient

class Logger {

    companion object {

        internal const val TAG = "Courier SDK"

        internal fun log(data: String) {
            Log.d(CourierClient.TAG, data)
        }

        internal fun warn(data: String) {
            Log.w(CourierClient.TAG, data)
        }

        internal fun error(data: String?) {
            val message = data ?: "Oops, an error occurred"
            Log.e(CourierClient.TAG, message)
        }

    }

}