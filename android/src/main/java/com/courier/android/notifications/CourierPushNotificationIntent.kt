package com.courier.android.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.courier.android.Courier

/**
 * Convenience intent for launching an Activity from a push notification tap.
 *
 * This wraps an [Intent] targeted at the given `Activity` and attaches the
 * original push payload so your Activity can retrieve it on launch.
 *
 * What it does:
 * - Sets `action = ACTION_MAIN` and adds `CATEGORY_LAUNCHER` so the intent
 *   behaves like a normal app launch from the launcher.
 * - Adds `FLAG_ACTIVITY_SINGLE_TOP` so taps route to the existing Activity
 *   instance (and trigger `onNewIntent`) instead of creating a new one.
 * - Puts the provided `message` into the extras under
 *   [Courier.COURIER_PENDING_NOTIFICATION_KEY] for later retrieval.
 * - Exposes a `pendingIntent` suitable for use in a notification.
 *
 * Notes:
 * - `pendingIntent` uses `FLAG_CANCEL_CURRENT` to replace any previous intent
 *   with the same request code, and `FLAG_MUTABLE` so extras can be inspected
 *   on Android 12+. If you do not need to modify the PendingIntent after
 *   creation, consider `FLAG_IMMUTABLE` for tighter security.
 * - Your target Activity should handle the payload in `onCreate` and
 *   `onNewIntent` to cover both cold-start and warm-start cases.
 *
 * Example:
 * ```
 * val intent = CourierPushNotificationIntent(
 *     context = context,
 *     target = MainActivity::class.java,
 *     message = remoteMessage // Parcelable payload usually an FCM RemoteMessage
 * )
 *
 * val notification = NotificationCompat.Builder(context, CHANNEL_ID)
 *     .setContentTitle("New message")
 *     .setContentText("Tap to view")
 *     .setContentIntent(intent.pendingIntent)
 *     .setAutoCancel(true)
 *     .build()
 * ```
 *
 * @param context Android context used to create the underlying [Intent] and [PendingIntent].
 * @param target  The Activity class to open when the user taps the notification.
 * @param payload Parcelable payload attached under [Courier.COURIER_PENDING_NOTIFICATION_KEY].
 *
 * @see android.content.Intent.ACTION_MAIN
 * @see android.content.Intent.CATEGORY_LAUNCHER
 * @see android.app.PendingIntent
 */
class CourierPushNotificationIntent(val context: Context, target: Class<*>?, payload: Parcelable) : Intent(context, target) {

    init {
        putExtra(Courier.COURIER_PENDING_NOTIFICATION_KEY, payload)
        addCategory(CATEGORY_LAUNCHER)
        addFlags(FLAG_ACTIVITY_SINGLE_TOP)
        action = ACTION_MAIN
    }

    val pendingIntent: PendingIntent
        get() = PendingIntent.getActivity(
            context,
            0,
            this,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        )

}