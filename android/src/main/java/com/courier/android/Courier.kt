package com.courier.android

import android.annotation.SuppressLint
import android.content.Context
import com.courier.android.managers.UserManager
import com.courier.android.models.CourierAgent
import com.courier.android.models.CourierException
import com.courier.android.models.CourierProvider
import com.courier.android.repositories.TokenRepository
import com.courier.android.utils.NotificationEventBus
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Courier private constructor() {

    companion object {

        var USER_AGENT = CourierAgent.NATIVE_ANDROID
        internal const val VERSION = "1.0.20"
        internal const val TAG = "Courier SDK"
        internal const val COURIER_PENDING_NOTIFICATION_KEY = "courier_pending_notification_key"
        internal val eventBus by lazy { NotificationEventBus() }
        internal val COURIER_COROUTINE_CONTEXT by lazy { Job() }
        internal val coroutineScope = CoroutineScope(COURIER_COROUTINE_CONTEXT)

        /**
         * Initializes the SDK with a static reference to a Courier singleton
         * This function must be called before you can use the Courier.shared value
         * Courier.shared is required for nearly all features of the SDK
         */
        fun initialize(context: Context) {
            if (mInstance == null) {
                mInstance = Courier()
            }
            mInstance?.context = context
        }

        // This will not create a memory leak
        // Please call Courier.initialize(context) before using Courier.shared
        @SuppressLint("StaticFieldLeak")
        private var mInstance: Courier? = null
        val shared: Courier get() {
            mInstance?.let { return it }
            throw CourierException.initializationError
        }

        // Returns the last message that was delivered via the event bus
        fun getLastDeliveredMessage(onMessageFound: (message: RemoteMessage) -> Unit) = coroutineScope.launch(Dispatchers.Main) {
            eventBus.events.collectLatest { message ->
                onMessageFound(message)
            }
        }

    }

    // Repos
    private val tokenRepo by lazy { TokenRepository() }

    /**
     * Shows or hides Android console logs
     */
    var isDebugging = false

    /**
     * Handles interfacing between shared preferences and the Courier SDK
     */
    private lateinit var context: Context

    /**
     * The key required to initialized the SDK
     * [Issue Tokens](https://www.courier.com/docs/reference/auth/issue-token/)
     */
    internal val accessToken: String? get() = UserManager.getAccessToken(context)

    /**
     * A read only value set to the current user id
     */
    val userId: String? get() = UserManager.getUserId(context)

    /**
     * Gets called if set and a log is posted
     */
    var logListener: ((data: String) -> Unit)? = null

    init {

        // Set app debugging
        isDebugging = BuildConfig.DEBUG

    }

    /**
     * Function to set the current credentials for the user and their access token
     * You should consider using this in areas where you update your local user's state
     */
    suspend fun signIn(accessToken: String, userId: String) = withContext(COURIER_COROUTINE_CONTEXT) {

        Courier.log("Updating Courier User Profile:\n" +
                "Access Token: $accessToken\n" +
                "User Id: $userId")

        // Update user manager
        UserManager.setCredentials(
            context = context,
            accessToken = accessToken,
            userId = userId
        )

        // Post the fcm token if we can
        // If this SDK supports more tokens
        // this return will need to change
        return@withContext getCurrentFcmToken()?.let { token ->
            setFCMToken(token)
        }

    }

    fun signIn(accessToken: String, userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
        try {
            signIn(
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
        // If there is no access token and messaging token, skip this
        val fcmToken = this@Courier.fcmToken ?: getCurrentFcmToken()
        if (accessToken != null && fcmToken != null) {
            tokenRepo.deleteUserToken(fcmToken)
        }

        // Remove credentials
        UserManager.removeCredentials(context)

    }

    fun signOut(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
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

        // Delete the previous token if possible
        // If we fail the delete, skip and move on
        // We want to ensure the user has a new token in Courier
        this@Courier.fcmToken?.let { currentToken ->
            try {
                tokenRepo.deleteUserToken(currentToken)
            } catch (e: Exception) {
                Courier.log(e.toString())
            }
        }

        // Set the new token and put in Courier
        this@Courier.fcmToken = token

        Courier.log("Firebase Cloud Messaging Token:\n$token")

        return tokenRepo.putUserToken(token, CourierProvider.FCM)

    }

    fun setFCMToken(token: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
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