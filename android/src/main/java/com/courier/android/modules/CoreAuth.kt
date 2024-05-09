package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.managers.UserManager
import com.courier.android.models.CourierAuthenticationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CoreAuth {

    private var listeners: MutableList<CourierAuthenticationListener> = mutableListOf()

    /**
     * Function to set the current credentials for the user and their access token
     * You should consider using this in areas where you update your local user's state
     */
    suspend fun signIn(userId: String, tenantId: String?, accessToken: String, clientKey: String?, push: CorePush, inbox: CoreInbox) = withContext(Dispatchers.IO) {

        // Sign user out if needed
        if (Courier.shared.isUserSignedIn) {
            Courier.shared.signOut()
        }

        Courier.log("Signing user in")
        Courier.log("User Id: $userId")
        Courier.log("Access Token: $accessToken")
        Courier.log("Client Key: $clientKey")

        // Set the current user
        UserManager.setCredentials(
            context = Courier.shared.context,
            userId = userId,
            accessToken = accessToken,
            clientKey = clientKey,
            tenantId = tenantId,
        )

        push.putPushTokens()
        inbox.restart()
        notifyListeners()

    }

    /**
     * Function that clears the current user id and access token
     * You should call this when your user signs out
     * It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
     */
    suspend fun signOut(push: CorePush, inbox: CoreInbox) = withContext(Dispatchers.IO) {

        // Ensure we have a user to sign out
        if (!Courier.shared.isUserSignedIn) {
            Courier.log("No user signed into Courier. A user must be signed in on order to sign out.")
            return@withContext
        }

        Courier.log("Signing user out")

        push.deletePushTokens()

        inbox.close()

        // Clear the user
        // Must be called after tokens are deleted
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
 * Token needed to authenticate with JWTs for GraphQL requests
 */
internal val Courier.jwt: String? get() = clientKey?.let { null } ?: accessToken

/**
 * A read only value set to the current tenant id
 */
val Courier.tenantId: String? get() = UserManager.getTenantId(context)

/**
 * Determine user state
 */
val Courier.isUserSignedIn get() = userId != null && accessToken != null

suspend fun Courier.signIn(userId: String, accessToken: String, clientKey: String? = null, tenantId: String? = null) {
    auth.signIn(
        userId = userId,
        accessToken = accessToken,
        clientKey = clientKey,
        tenantId = tenantId,
        push = push,
        inbox = inbox,
    )
}

fun Courier.signIn(userId: String, accessToken: String, clientKey: String? = null, tenantId: String? = null, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.IO) {
    try {
        signIn(
            userId = userId,
            accessToken = accessToken,
            clientKey = clientKey,
            tenantId = tenantId,
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
        push = push,
        inbox = inbox
    )
}

fun Courier.signOut(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.Main) {
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