package com.courier.android.modules

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.courier.android.Courier
import com.courier.android.client.error
import com.courier.android.client.log
import com.courier.android.models.CourierException
import com.courier.android.models.CourierPushProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// Get the token but will timeout after some time if the token is not found
// This is some sort of issue with Firebase Messaging and Emulators
private suspend fun Courier.getFcmToken(timeout: Long = 8000): String? {

    // Ensure firebase is setup
    if (!isFirebaseInitialized) {
        client?.options?.error("Firebase is not initialized. Courier will not be able to get the FCM token until Firebase is initialized.")
        return null
    }

    // Check for push permissions
    if (!Courier.shared.isPushPermissionGranted(Courier.shared.context)) {
        return null
    }

    // Get token if possible
    // Timeout if needed
    return try {
        return withTimeout(timeout) {
            return@withTimeout Firebase.messaging.token.await()
        }
    } catch (e: Exception) {
        client?.options?.error(e.toString())
        null
    }

}

internal suspend fun Courier.setFCMToken(newToken: String) {

    if (Courier.shared.accessToken == null || Courier.shared.userId == null) {
        fcmToken = newToken
        return
    }

    // FCM key
    val key = CourierPushProvider.FIREBASE_FCM.value

    // Remove the old token
    deleteTokenIfNeeded(
        token = tokens[key]
    )

    // Save the local token
    fcmToken = newToken

    // Put the new token
    putToken(
        provider = key,
        token = newToken
    )

}

suspend fun Courier.setToken(provider: String, token: String) {

    if (Courier.shared.accessToken == null || Courier.shared.userId == null) {
        tokens[provider] = token
        return
    }

    // Remove the old token
    deleteTokenIfNeeded(
        token = tokens[provider]
    )

    // Save the local token
    tokens[provider] = token

    // Put the new token
    putToken(
        provider = provider,
        token = token
    )

}

suspend fun Courier.setToken(provider: CourierPushProvider, token: String) {
    setToken(
        provider = provider.value,
        token = token
    )
}

suspend fun Courier.refreshFcmToken() = withContext(Dispatchers.IO) {
    if (fcmToken == null) {
        fcmToken = getFcmToken()
    }
}

internal suspend fun Courier.putPushTokens() = withContext(Dispatchers.IO) {

    // Check if we do not have an fcm token
    refreshFcmToken()

    // Save all the tokens
    tokens.forEach { token ->
        putTokenIfNeeded(
            provider = token.key,
            token = token.value
        )
    }

}

// Delete all the current tokens for the user
// Done like this in case we have other tokens for the user at some point
internal suspend fun Courier.deletePushTokens() = withContext(Dispatchers.IO) {

    // Check if we do not have an fcm token
    refreshFcmToken()

    // Remove the old tokens
    tokens.forEach { token ->
        deleteTokenIfNeeded(
            token = token.value
        )
    }

}

private suspend fun Courier.putToken(provider: String, token: String) {

    if (Courier.shared.accessToken == null || Courier.shared.userId == null) {
        throw CourierException.missingAccessToken
    }

    client?.options?.log("Putting $provider Token: $token")

    client?.tokens?.putUserToken(
        token = token,
        provider = provider
    )

}

private suspend fun Courier.putTokenIfNeeded(provider: String, token: String?) {

    if (Courier.shared.accessToken == null || Courier.shared.userId == null || token == null) {
        return
    }

    try {
        putToken(
            provider = provider,
            token = token
        )
    } catch (e: Exception) {
        client?.options?.log(e.toString())
    }

}

private suspend fun Courier.deleteToken(token: String) {

    if (Courier.shared.accessToken == null || Courier.shared.userId == null) {
        throw CourierException.missingAccessToken
    }

    client?.options?.log("Deleting Token: $token")

    client?.tokens?.deleteUserToken(
        token = token
    )

}

private suspend fun Courier.deleteTokenIfNeeded(token: String?) {

    if (Courier.shared.accessToken == null || Courier.shared.userId == null || token == null) {
        return
    }

    try {
        deleteToken(
            token = token
        )
    } catch (e: Exception) {
        client?.options?.log(e.toString())
    }

}

/**
 * Getters
 */

val Courier.fcmToken get() = fcmToken

/**
 * Traditional Callbacks
 */

fun Courier.setFCMToken(token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setFCMToken(token)
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

fun Courier.setToken(provider: String, token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setToken(provider, token)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

fun Courier.setToken(provider: CourierPushProvider, token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setToken(provider, token)
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