package com.courier.android

import com.courier.android.models.CourierException
import com.courier.android.models.CourierProvider
import com.courier.android.repositories.TokenRepository
import com.courier.android.utils.NotificationEventBus
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Courier private constructor() {

    companion object {
        internal const val TAG = "Courier"
        internal val COURIER_COROUTINE_CONTEXT by lazy { Job() }
        const val COURIER_PENDING_NOTIFICATION_KEY = "courier_pending_notification_key"
        internal val eventBus by lazy { NotificationEventBus() }
        val instance = Courier()
    }

    var isDebugging = false

    // Repos
    private val tokenRepo by lazy { TokenRepository() }

    /**
     * The key required to initialized the SDK
     * [Issue Tokens](https://www.courier.com/docs/reference/auth/issue-token/)
     */
    internal var accessToken: String? = null
        private set

    /**
     * A read only value set to the current user id
     */
    var userId: String? = null
        private set

    init {

        // Set app debugging
        if (BuildConfig.DEBUG) {
            isDebugging = true
        }

    }

    /**
     * Function to set the current credentials for the user and their access token
     * You should consider using this in areas where you update your local user's state
     */
    suspend fun setCredentials(accessToken: String, userId: String) = withContext(COURIER_COROUTINE_CONTEXT) {

        Courier.log("Updating Courier User Profile")
        Courier.log("Access Token: $accessToken")
        Courier.log("User Id: $userId")

        // Set the user's current credentials
        this@Courier.accessToken = accessToken
        this@Courier.userId = userId

        // Try and grab the users current fcm token
        this@Courier.fcmToken = getCurrentFcmToken()

        // Post the fcm token if we can
        // If this SDK supports more tokens
        // this return will need to change
        return@withContext fcmToken?.let { token ->
            setFCMToken(token)
        }

    }

    fun setCredentials(accessToken: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
        try {
            setCredentials(
                accessToken = accessToken,
                userId = userId
            )
            onSuccess()
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    /**
     * Function that clears the current user id and access token
     * You should call this when your user signs out
     * It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
     */
    suspend fun signOut() = withContext(COURIER_COROUTINE_CONTEXT) {

        Courier.log("Clearing Courier User Credentials")

        // Attempt to delete the current fcm token from the user
        this@Courier.fcmToken?.let { token ->
            tokenRepo.deleteUserToken(token)
        }

        this@Courier.accessToken = null
        this@Courier.userId = null

    }

    fun signOut(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
        try {
            signOut()
            onSuccess()
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    /**
     * The current firebase token associated with this user
     */
    var fcmToken: String? = null
        private set

    /**
     * Upserts the FCM token in Courier for the current user
     * To get started with FCM, checkout the firebase docs [Here](https://firebase.google.com/docs/cloud-messaging/android/client)
     */
    suspend fun setFCMToken(token: String) {

        this@Courier.fcmToken = token

        Courier.log("Firebase Cloud Messaging Token")
        Courier.log(token)

        return tokenRepo.putUserToken(token, CourierProvider.FCM)

    }

    fun setFCMToken(token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = CoroutineScope(COURIER_COROUTINE_CONTEXT).launch(Dispatchers.IO) {
        try {
            setFCMToken(token)
            onSuccess()
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    private suspend fun getCurrentFcmToken() = suspendCoroutine { continuation ->

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->

            if (!task.isSuccessful) {
                Courier.log(task.exception.toString())
                continuation.resume(null)
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            continuation.resume(fcmToken)

        }

    }

}