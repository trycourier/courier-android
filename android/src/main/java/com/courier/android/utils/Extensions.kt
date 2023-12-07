package com.courier.android.utils

import android.Manifest
import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.app.ActivityCompat
import com.courier.android.Courier
import com.courier.android.Courier.Companion.COURIER_COROUTINE_CONTEXT
import com.courier.android.Courier.Companion.eventBus
import com.courier.android.inbox.CourierInboxFont
import com.courier.android.models.CourierPushEvent
import com.courier.android.repositories.MessagingRepository
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

fun Intent.trackPushNotificationClick(onClick: (message: RemoteMessage) -> Unit) {

    try {

        // Check to see if we have an intent to work
        val key = Courier.COURIER_PENDING_NOTIFICATION_KEY
        (extras?.get(key) as? RemoteMessage)?.let { message ->

            // Clear the intent extra
            extras?.remove(key)

            // Track when the notification was clicked
            Courier.shared.trackNotification(
                message = message,
                event = CourierPushEvent.CLICKED,
                onSuccess = { Courier.log("Event tracked") },
                onFailure = { Courier.error(it.toString()) }
            )

            onClick(message)

        }

    } catch (e: Exception) {

        Courier.error(e.toString())

    }

}

suspend fun Courier.trackNotification(message: RemoteMessage, event: CourierPushEvent) = withContext(COURIER_COROUTINE_CONTEXT) {
    val trackingUrl = message.data["trackingUrl"] ?: return@withContext
    MessagingRepository().postTrackingUrl(
        url = trackingUrl,
        event = event
    )
}

fun Courier.trackNotification(message: RemoteMessage, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        Courier.shared.trackNotification(
            message = message,
            event = event
        )
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun Courier.trackNotification(trackingUrl: String, event: CourierPushEvent) = withContext(COURIER_COROUTINE_CONTEXT) {
    MessagingRepository().postTrackingUrl(
        url = trackingUrl,
        event = event
    )
}

fun Courier.trackNotification(trackingUrl: String, event: CourierPushEvent, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        Courier.shared.trackNotification(
            trackingUrl = trackingUrl,
            event = event
        )
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

internal fun Courier.broadcastMessage(message: RemoteMessage) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        eventBus.emitEvent(message)
    } catch (e: Exception) {
        Courier.error(e.toString())
    }
}

// Using this function, you will need to manually handle the result
// Inside `Activity.onActivityResult`
fun Context.requestNotificationPermission(requestCode: Int = 1) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        if (!isPushPermissionGranted) {

            if (this is Activity) {
                val permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                requestPermissions(permissions, requestCode)
            }

        }

    }

}

val Context.isPushPermissionGranted: Boolean get() {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

}

val RemoteMessage.pushNotification: Map<String, Any?>
    get() {

        val rawData = data.toMutableMap()
        val payload = mutableMapOf<String, Any?>()

        // Add existing values to base map
        // then remove the unneeded keys
        val baseKeys = listOf("title", "subtitle", "body", "badge", "sound")
        baseKeys.forEach { key ->
            payload[key] = data[key]
            rawData.remove(key)
        }

        // Add extras
        for ((key, value) in rawData) {
            payload[key] = value
        }

        // Add the raw data
        payload["raw"] = data

        return payload

    }

// Returns the last message that was delivered via the event bus
fun Courier.getLastDeliveredMessage(onMessageFound: (message: RemoteMessage) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    eventBus.events.collectLatest { message ->
        onMessageFound(message)
    }
}

internal fun String.isoToDate(): Date? {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    isoFormat.timeZone = TimeZone.getTimeZone("UTC")
    return isoFormat.parse(this)
}

internal fun Date.toIsoTimestamp(): String {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    isoFormat.timeZone = TimeZone.getTimeZone("UTC")
    return isoFormat.format(this)
}

internal fun Date.timeSince(): String {

    val secondsAgo = (Date().time - time) / 1000

    val minute = 60
    val hour = 60 * minute
    val day = 24 * hour
    val week = 7 * day
    val month = 4 * week
    val year = 12 * month

    val secondString = "s"
    val minuteString = "m"
    val hourString = "h"
    val dayString = "d"
    val weekString = "w"
    val yearString = "y"

    return if (secondsAgo <= 0) {
        "now"
    } else if (secondsAgo < minute) {
        "${secondsAgo}${secondString}"
    } else if (secondsAgo < hour) {
        "${secondsAgo / minute}${minuteString}"
    } else if (secondsAgo < day) {
        "${secondsAgo / hour}${hourString}"
    } else if (secondsAgo < week) {
        "${secondsAgo / day}${dayString}"
    } else if (secondsAgo < year) {
        "${secondsAgo / week}${weekString}"
    } else {
        "${secondsAgo / year}${yearString}"
    }

}

/**
 * Converts Pixel to DP.
 */
internal val Int.pxToDp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()

/**
 * Converts DP to Pixel.
 */
internal val Int.dpToPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

internal val Context.isDarkMode: Boolean
    get() {
        val darkModeFlag = resources.configuration.uiMode and UI_MODE_NIGHT_MASK
        return darkModeFlag == UI_MODE_NIGHT_YES
    }

internal fun Context.getColorFromAttribute(resId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(resId, typedValue, true)
    return typedValue.data
}

internal fun isDarkModeOn(context: Context): Boolean {
    val currentNightMode = context.resources.configuration.uiMode and UI_MODE_NIGHT_MASK
    return currentNightMode == UI_MODE_NIGHT_YES
}

internal fun TextView.setCourierFont(font: CourierInboxFont?, @ColorInt fallbackColor: Int? = null) {

    // Typeface
    font?.typeface?.let {
        typeface = it
    }

    // Color
    (font?.color ?: fallbackColor)?.let {
        setTextColor(it)
    }

    // Text Size
    font?.sizeInSp?.let {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
    }

}