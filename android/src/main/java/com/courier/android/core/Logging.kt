package com.courier.android.core

import android.util.Log

internal class Logging {

    companion object {

        private const val TAG = "Courier SDK"

        internal fun log(data: String) {
            Log.d(TAG, data)
        }

        internal fun warn(data: String) {
            Log.w(TAG, data)
        }

        internal fun error(data: String?) {
            val message = data ?: "Oops, an error occurred"
            Log.e(TAG, message)
        }

    }

}