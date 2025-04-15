package com.courier.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.Courier.Companion.eventBus
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierException
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.models.CourierPushNotificationEvent
import com.courier.android.ui.CourierStyles
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.internal.toHexString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Intent.trackPushNotificationClick(onClick: (message: RemoteMessage) -> Unit) {

    try {

        // Check to see if we have an intent to work
        val key = Courier.COURIER_PENDING_NOTIFICATION_KEY
        @Suppress("DEPRECATION")
        (extras?.get(key) as? RemoteMessage)?.let { message ->

            // Clear the intent extra
            extras?.remove(key)

            // Broadcast and track the event
            Courier.shared.trackAndBroadcastTheEvent(
                trackingEvent = CourierTrackingEvent.CLICKED,
                message = message
            )

            onClick(message)

        }

    } catch (e: Exception) {

        CourierClient.default.error(e.toString())

    }

}

internal fun Courier.trackAndBroadcastTheEvent(trackingEvent: CourierTrackingEvent, message: RemoteMessage) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {

        // Track the notification
        message.data["trackingUrl"]?.let { trackingUrl ->
            coroutineScope.launch(Dispatchers.IO) {
                CourierClient.default.tracking.postTrackingUrl(
                    url = trackingUrl,
                    event = trackingEvent,
                )
            }
        }

        // Broadcast the event
        eventBus.onPushNotificationEvent(trackingEvent, message)

    } catch (e: Exception) {
        Courier.shared.client?.error(e.toString())
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
fun Courier.onPushNotificationEvent(onEvent: (event: CourierPushNotificationEvent) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    eventBus.events.collectLatest {
        onEvent(it)
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

internal fun TextView.setCourierFont(font: CourierStyles.Font?, @ColorInt fallbackColor: Int? = null) {

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

    if (Courier.shared.isUITestsActive) {
        tag = "TextView, fontTypeface: ${font?.typeface}, fontColor: ${font?.color?.toHexString()}, fontSize: ${font?.sizeInSp}"
    }

}

internal fun Context.launchCourierWebsite() {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.courier.com/"))
    startActivity(browserIntent)
}

internal val Exception.toCourierException get() = CourierException(message ?: "Unknown Error")

@SuppressLint("NotifyDataSetChanged")
internal fun RecyclerView.forceReactNativeLayoutFix() {

    // Break if not React Native
    if (!Courier.agent.isReactNative) {
        return
    }

    try {

        // Forces the layout to refresh
        // This is a react native bug
        adapter?.notifyDataSetChanged()
        scrollBy(0, 0)

    } catch (e: Exception) {

        Courier.shared.client?.error(e.toString())

    }

}

fun ColorDrawable.toHex() = String.format(Locale.getDefault(), "#%06X", color)
