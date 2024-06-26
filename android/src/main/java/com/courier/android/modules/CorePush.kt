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
import com.courier.android.repositories.UsersRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class CorePush {

    private val usersRepo by lazy { UsersRepository() }

    // Ensures we can use firebase functions
    private val isFirebaseInitialized get() = FirebaseApp.getApps(Courier.shared.context).isNotEmpty()

    // Keep a reference to all tokens
    internal var tokens: MutableMap<String, String> = mutableMapOf()

    // Stores a local copy of the fcmToken
    internal var fcmToken: String? = null
        set(value) {

            // Set the value
            field = value

            val key = CourierPushProvider.FIREBASE_FCM.value

            // Update the local cache
            if (value != null) {
                tokens[key] = value
            } else {
                tokens.remove(key)
            }

        }

    // Get the token but will timeout after some time if the token is not found
    // This is some sort of issue with Firebase Messaging and Emulators
    private suspend fun getFcmToken(timeout: Long = 8000): String? {

        // Ensure firebase is setup
        if (!isFirebaseInitialized) {
            Courier.error("Firebase is not initialized. Courier will not be able to get the FCM token until Firebase is initialized.")
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
            Courier.error(e.toString())
            null
        }

    }

    internal suspend fun setFCMToken(newToken: String) {

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

    internal suspend fun setToken(provider: String, token: String) {

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

    internal suspend fun refreshFcmToken() = withContext(Dispatchers.IO) {
        if (fcmToken == null) {
            fcmToken = getFcmToken()
        }
    }

    internal suspend fun putPushTokens() = withContext(Dispatchers.IO) {

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
    internal suspend fun deletePushTokens() = withContext(Dispatchers.IO) {

        // Check if we do not have an fcm token
        refreshFcmToken()

        // Remove the old tokens
        tokens.forEach { token ->
            deleteTokenIfNeeded(
                token = token.value
            )
        }

    }

    private suspend fun putToken(provider: String, token: String) {

        if (Courier.shared.accessToken == null || Courier.shared.userId == null) {
            throw CourierException.missingAccessToken
        }

        Courier.log("Putting $provider Token: $token")

        usersRepo.putUserToken(
            accessToken = Courier.shared.accessToken!!,
            userId = Courier.shared.userId!!,
            token = token,
            provider = provider
        )

    }

    private suspend fun putTokenIfNeeded(provider: String, token: String?) {

        if (Courier.shared.accessToken == null || Courier.shared.userId == null || token == null) {
            return
        }

        try {
            putToken(
                provider = provider,
                token = token
            )
        } catch (e: Exception) {
            Courier.log(e.toString())
        }

    }

    private suspend fun deleteToken(token: String) {

        if (Courier.shared.accessToken == null || Courier.shared.userId == null) {
            throw CourierException.missingAccessToken
        }

        Courier.log("Deleting Token: $token")

        usersRepo.deleteUserToken(
            accessToken = Courier.shared.accessToken!!,
            userId = Courier.shared.userId!!,
            token = token
        )

    }

    private suspend fun deleteTokenIfNeeded(token: String?) {

        if (Courier.shared.accessToken == null || Courier.shared.userId == null || token == null) {
            return
        }

        try {
            deleteToken(
                token = token
            )
        } catch (e: Exception) {
            Courier.log(e.toString())
        }

    }

}

/**
 * Extensions
 */

/**
 * Get the FCM Token
 */

val Courier.fcmToken get() = push.fcmToken

/**
 * Upserts the FCM token in Courier for the current user
 * To get started with FCM, checkout the firebase docs [Here](https://firebase.google.com/docs/cloud-messaging/android/client)
 */
suspend fun Courier.setFCMToken(token: String) {
    push.setFCMToken(token)
}

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
fun Courier.getToken(provider: String) = push.tokens[provider]

fun Courier.getToken(provider: CourierPushProvider) = push.tokens[provider.value]

/**
 * Sets the current token for the provider
 */

suspend fun Courier.setToken(provider: String, token: String) {
    push.setToken(provider = provider, token = token)
}

fun Courier.setToken(provider: String, token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        setToken(provider, token)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun Courier.setToken(provider: CourierPushProvider, token: String) {
    push.setToken(provider = provider.value, token = token)
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