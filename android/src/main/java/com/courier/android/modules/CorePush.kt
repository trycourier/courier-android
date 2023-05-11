package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.repositories.TokenRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.*

internal class CorePush {

    private val tokenRepo by lazy { TokenRepository() }
//    private lateinit var trackingRepo = TrackingRepository()

    // Stores a local copy of the fcmToken
    private var fcmToken: String? = null

    // Ensures we can use firebase functions
    private val isFirebaseInitialized get() = FirebaseApp.getApps(Courier.shared.context).isNotEmpty()

    internal suspend fun getFcmToken(): String? {

        if (!isFirebaseInitialized) {
            Courier.error("Firebase is not initialized. Courier will not be able to get the FCM token until Firebase is initialized.")
            return null
        }

        // Get the existing fcm token
        fcmToken?.let { existingToken ->
            return existingToken
        }

        // Get the latest token
        val latestFcmToken = suspendCoroutine { continuation ->

            // Get the current FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Courier.error(task.exception.toString())
                }

                // Save and return the token
                continuation.resume(task.result)

            }

        }

        // Update the fcm token
        fcmToken = latestFcmToken

        return latestFcmToken

    }

    suspend fun setFCMToken(newToken: String?) {

        Courier.log("Firebase Cloud Messaging Token: $newToken")

        // Delete the old token if possible
        val oldToken = getFcmToken()
        deleteToken(oldToken)

        // Set the new token
        fcmToken = newToken

        // Put the new token
        putToken(CourierProvider.FCM, newToken)

    }

    internal suspend fun putPushTokens() = withContext(Courier.COURIER_COROUTINE_CONTEXT) {

        // Refresh the tokens
        fcmToken = getFcmToken()

        // Put the new tokens in Courier
        putToken(CourierProvider.FCM, fcmToken)

    }

    // Delete all the current tokens for the user
    // Done like this in case we have other tokens for the user at some point
    internal suspend fun deletePushTokens() = withContext(Courier.COURIER_COROUTINE_CONTEXT) {

        // Refresh the tokens
        fcmToken = getFcmToken()

        // Remove the old tokens
        deleteToken(fcmToken)

    }

    // Tries add the token from Courier
    // Will silently fail if error occurs
    private suspend fun putToken(provider: CourierProvider, token: String?) {

        if (Courier.shared.accessToken == null || Courier.shared.userId == null || token == null) {
            return
        }

        Courier.log("Putting ${provider.value} Messaging Token: $token")

        try {
            tokenRepo.putUserToken(
                accessToken = Courier.shared.accessToken!!,
                userId = Courier.shared.userId!!,
                token = token,
                provider = provider
            )
        } catch (e: Exception) {
            Courier.error(e.message)
        }

    }

    // Tries to the remove the token from Courier
    // Will silently fail if error occurs
    private suspend fun deleteToken(token: String?) {

        if (Courier.shared.accessToken == null || Courier.shared.userId == null || token == null) {
            return
        }

        Courier.log("Deleting Messaging Token: $token")

        try {
            tokenRepo.deleteUserToken(
                accessToken = Courier.shared.accessToken!!,
                userId = Courier.shared.userId!!,
                token = token
            )
        } catch (e: Exception) {
            Courier.error(e.message)
        }

    }

}

/**
 * Extensions
 */

/**
 * The current firebase cloud messaging token for the device
 */
suspend fun Courier.getFCMToken() = push.getFcmToken()

fun Courier.getFCMToken(onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        val token = getFCMToken()
        onSuccess(token)
    } catch (e: Exception) {
        onFailure(e)
    }
}

/**
 * Upserts the FCM token in Courier for the current user
 * To get started with FCM, checkout the firebase docs [Here](https://firebase.google.com/docs/cloud-messaging/android/client)
 */
suspend fun Courier.setFCMToken(token: String?) {
    push.setFCMToken(token)
}

fun Courier.setFCMToken(token: String?, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        setFCMToken(token)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}