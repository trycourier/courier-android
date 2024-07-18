package com.courier.android.utils

import android.util.Log
import com.courier.android.client.CourierClient

class Logger {

    companion object {

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

/**
 * Extensions
 */

internal fun CourierClient.Options.log(data: String) {
    if (showLogs) {
        Logger.log(data)
    }
}

internal fun CourierClient.Options.warn(data: String) {
    if (showLogs) {
        Logger.warn(data)
    }
}

internal fun CourierClient.Options.error(data: String?) {
    if (showLogs) {
        val message = data ?: "Oops, an error occurred"
        Logger.error(message)
    }
}

/**
 * Courier Client Extensions
 */

fun CourierClient.log(data: String) = options.log(data)
fun CourierClient.warn(data: String) = options.warn(data)
fun CourierClient.error(data: String?) = options.error(data)