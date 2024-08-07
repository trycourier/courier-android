package com.courier.android.modules

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.courier.android.Courier
import com.courier.android.models.CourierException
import com.courier.android.models.CourierPushProvider
import com.courier.android.utils.error
import com.courier.android.utils.log
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Get the token but will timeout after some time if the token is not found
// This is some sort of issue with Firebase Messaging and Emulators
private suspend fun Courier.getFcmToken(context: Context, timeout: Long): String? {

    // Ensure firebase is setup
    if (!isFirebaseInitialized(context)) {
        client?.error("Firebase is not initialized. Courier will not be able to get the FCM token until Firebase is initialized.")
        return null
    }

    // Check for push permissions
    if (!Courier.shared.isPushPermissionGranted(context)) {
        return null
    }

    // Get token if possible
    // Timeout if needed
    return try {
        return withTimeout(timeout) {
            return@withTimeout Firebase.messaging.token.await()
        }
    } catch (e: Exception) {
        client?.error(e.toString())
        null
    }

}

suspend fun Courier.setFcmToken(context: Context, token: String) {

    val key = CourierPushProvider.FIREBASE_FCM.value

    if (Courier.shared.getAccessToken(context) == null || Courier.shared.getUserId(context) == null) {
        fcmToken = token
        tokens[key] = token
        return
    }

    // Remove the old token
    deleteTokenIfNeeded(
        context = context,
        token = tokens[key]
    )

    // Save the local token
    fcmToken = token
    tokens[key] = token

    // Put the new token
    putToken(
        context = context,
        provider = key,
        token = token
    )

}

suspend fun Courier.setToken(context: Context, provider: String, token: String) {

    if (Courier.shared.getAccessToken(context) == null || Courier.shared.getUserId(context) == null) {
        tokens[provider] = token
        return
    }

    // Remove the old token
    deleteTokenIfNeeded(
        context = context,
        token = tokens[provider]
    )

    // Save the local token
    tokens[provider] = token

    // Put the new token
    putToken(
        context = context,
        provider = provider,
        token = token
    )

}

suspend fun Courier.setToken(context: Context, provider: CourierPushProvider, token: String) {
    setToken(
        context = context,
        provider = provider.value,
        token = token
    )
}

suspend fun Courier.refreshFcmToken(context: Context, timeout: Long = 8000) = withContext(Dispatchers.IO) {
    if (fcmToken == null) {
        getFcmToken(context, timeout)?.let { newToken ->
            setFcmToken(context, newToken)
        }
    }
}

internal suspend fun Courier.putPushTokens(context: Context) = withContext(Dispatchers.IO) {

    // Check if we do not have an fcm token
    refreshFcmToken(context)

    // Save all the tokens
    tokens.forEach { token ->
        putTokenIfNeeded(
            context = context,
            provider = token.key,
            token = token.value
        )
    }

}

// Delete all the current tokens for the user
// Done like this in case we have other tokens for the user at some point
internal suspend fun Courier.deletePushTokens(context: Context) = withContext(Dispatchers.IO) {

    // Check if we do not have an fcm token
    refreshFcmToken(context)

    // Remove the old tokens
    tokens.forEach { token ->
        deleteTokenIfNeeded(
            context = context,
            token = token.value
        )
    }

}

private suspend fun Courier.putToken(context: Context, provider: String, token: String) {

    if (Courier.shared.getAccessToken(context) == null || Courier.shared.getUserId(context) == null) {
        throw CourierException.missingAccessToken
    }

    client?.log("Putting $provider Token: $token")

    client?.tokens?.putUserToken(
        token = token,
        provider = provider
    )

}

private suspend fun Courier.putTokenIfNeeded(context: Context, provider: String, token: String?) {

    if (Courier.shared.getAccessToken(context) == null || Courier.shared.getUserId(context) == null || token == null) {
        return
    }

    try {
        putToken(
            context = context,
            provider = provider,
            token = token
        )
    } catch (e: Exception) {
        client?.log(e.toString())
    }

}

private suspend fun Courier.deleteToken(context: Context, token: String) {

    if (Courier.shared.getAccessToken(context) == null || Courier.shared.getUserId(context) == null) {
        throw CourierException.missingAccessToken
    }

    client?.log("Deleting Token: $token")

    client?.tokens?.deleteUserToken(
        token = token
    )

}

private suspend fun Courier.deleteTokenIfNeeded(context: Context, token: String?) {

    if (Courier.shared.getAccessToken(context) == null || Courier.shared.getUserId(context) == null || token == null) {
        return
    }

    try {
        deleteToken(
            context = context,
            token = token
        )
    } catch (e: Exception) {
        client?.log(e.toString())
    }

}

/**
 * Getters
 */

val Courier.fcmToken get() = fcmToken

/**
 * Traditional Callbacks
 */

fun Courier.setFcmToken(context: Context, token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setFcmToken(context, token)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

/**
 * Gets the current token for the user
 * This value is stored locally
 */
fun Courier.getToken(provider: String) = tokens[provider]

fun Courier.getToken(provider: CourierPushProvider) = tokens[provider.value]

fun Courier.setToken(context: Context, provider: String, token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setToken(context, provider, token)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

fun Courier.setToken(context: Context, provider: CourierPushProvider, token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setToken(context, provider, token)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

fun Courier.requestNotificationPermission(activity: Activity, requestCode: Int = 1) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
    }
}

fun Courier.isPushPermissionGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}