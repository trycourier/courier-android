package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.friendlyMessage
import com.courier.android.managers.UserManager
import com.courier.android.models.CourierAuthenticationListener
import com.courier.android.models.CourierException
import kotlinx.coroutines.*

internal class CoreAuth {

    private var listeners: MutableList<CourierAuthenticationListener> = mutableListOf()

    /**
     * Function to set the current credentials for the user and their access token
     * You should consider using this in areas where you update your local user's state
     */
    suspend fun signIn(accessToken: String, clientKey: String?, userId: String, push: CorePush) = withContext(Courier.COURIER_COROUTINE_CONTEXT) {

        Courier.log("Signing user in")
        Courier.log("Access Token: $accessToken")
        Courier.log("Client Key: $clientKey")
        Courier.log("User Id: $userId")

        // Set the current user
        UserManager.setCredentials(
            context = Courier.shared.context,
            userId = userId,
            accessToken = accessToken,
            clientKey = clientKey
        )

        try {
            push.putPushTokens()
            // inbox.restartInboxIfNeeded()
            notifyListeners()
        } catch (e: Exception) {
            Courier.log(e.friendlyMessage)
            signOut(push)
            throw e
        }

    }

    /**
     * Function that clears the current user id and access token
     * You should call this when your user signs out
     * It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
     */
    suspend fun signOut(push: CorePush) = withContext(Courier.COURIER_COROUTINE_CONTEXT) {

        Courier.log("Signing user out")

        // Delete the push tokens
        push.deletePushTokens()

        // Clear the user
        UserManager.removeCredentials(Courier.shared.context)

        notifyListeners()

    }

    internal fun addAuthChangeListener(onChange: (String?) -> Unit): CourierAuthenticationListener {
        val listener = CourierAuthenticationListener(onChange)
        listeners.add(listener)
        return listener
    }

    internal fun removeAuthenticationListener(listener: CourierAuthenticationListener) {
        listeners.removeAll { it == listener }
    }

    private fun notifyListeners() {
        listeners.forEach { it.onChange(Courier.shared.userId) }
    }

}

/**
 * Extensions
 */

/**
 * The key required to initialized the SDK
 * https://app.courier.com/settings/api-keys
 * or
 * https://www.courier.com/docs/reference/auth/issue-token/
 */
internal val Courier.accessToken: String? get() = UserManager.getAccessToken(context)

/**
 * A read only value set to the current user id
 */
val Courier.userId: String? get() = UserManager.getUserId(context)

/**
 * A read only value set to the current user client key
 * https://app.courier.com/channels/courier
 */
internal val Courier.clientKey: String? get() = UserManager.getClientKey(context)

/**
 * Determine user state
 */
val Courier.isUserSignedIn get() = userId != null && accessToken != null

suspend fun Courier.signIn(accessToken: String, userId: String, clientKey: String? = null) {
    auth.signIn(
        accessToken = accessToken,
        userId = userId,
        clientKey = clientKey,
        push = push
    )
}

fun Courier.signIn(accessToken: String, userId: String, clientKey: String? = null, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
    try {
        signIn(
            accessToken = accessToken,
            userId = userId,
            clientKey = clientKey
        )
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

/**
Clears the current user id and access token
You should call this when your user signs out
It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
 */
suspend fun Courier.signOut() {
    auth.signOut(
        push = push
    )
}

fun Courier.signOut(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
    try {
        signOut()
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

/**
Gets called when the Authentication state for the current user changes in Courier
 */
fun Courier.addAuthenticationListener(onChange: (String?) -> Unit): CourierAuthenticationListener {
    return auth.addAuthChangeListener(onChange)
}