package com.courier.android.modules

import android.util.Log
import com.courier.android.BuildConfig
import com.courier.android.Courier

internal class CoreLogging {

    /**
     * Shows or hides Android console logs
     */
    internal var isDebugging = false

    /**
     * Gets called if set and a log is posted
     */
    internal var logListener: ((data: String) -> Unit)? = null

    init {

        // Set app debugging
        isDebugging = BuildConfig.DEBUG

    }

    internal fun log(data: String) {
        if (isDebugging) {
            Log.d(Courier.TAG, data)
            logListener?.invoke(data)
        }
    }

    internal fun warn(data: String) {
        if (isDebugging) {
            Log.w(Courier.TAG, data)
            logListener?.invoke(data)
        }
    }

    internal fun error(data: String?) {
        if (isDebugging) {
            val message = data ?: "Oops, an error occured"
            Log.e(Courier.TAG, message)
            logListener?.invoke(message)
        }
    }

}

/**
 * Extensions
 */

/**
 * Determines if the SDK should show logs or other debugging data
 * Set to find debug mode by default
 */
var Courier.isDebugging: Boolean
    get() = logging.isDebugging
    set(value) {
        logging.isDebugging = value
    }

// Called when logs are performed
// Used for React Native and Flutter SDKs
var Courier.logListener: ((String) -> Unit)?
    get() = logging.logListener
    set(value) {
        logging.logListener = value
    }