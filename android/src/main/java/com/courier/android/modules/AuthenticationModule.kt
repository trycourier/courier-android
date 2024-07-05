package com.courier.android.modules

import com.courier.android.BuildConfig
import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.client.log
import com.courier.android.managers.UserManager
import com.courier.android.models.CourierAuthenticationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Function to set the current credentials for the user and their access token
 * You should consider using this in areas where you update your local user's state
 */
suspend fun Courier.signIn(userId: String, tenantId: String? = null, accessToken: String, clientKey: String? = null, showLogs: Boolean = BuildConfig.DEBUG) = withContext(Dispatchers.IO) {

    // Sign user out if needed
    if (Courier.shared.isUserSignedIn) {
        Courier.shared.signOut()
    }

    // Generate a new connection id
    // Used for inbox socket
    val connectionId = UUID.randomUUID().toString()

    // Create the client
    client = CourierClient(
        jwt = accessToken,
        clientKey = clientKey,
        userId = userId,
        connectionId = connectionId,
        tenantId = tenantId,
        showLogs = showLogs,
    )

    client?.options?.log("Signing user in")
    client?.options?.log("User Id: $userId")
    client?.options?.log("Access Token: $accessToken")
    client?.options?.log("Client Key: $clientKey")

    // Set the current user
    UserManager.setCredentials(
        context = Courier.shared.context,
        userId = userId,
        accessToken = accessToken,
        clientKey = clientKey,
        tenantId = tenantId,
    )

    putPushTokens()
    refreshInbox()
    notifyListeners()

}

/**
 * Function that clears the current user id and access token
 * You should call this when your user signs out
 * It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
 */
suspend fun Courier.signOut() = withContext(Dispatchers.IO) {

    // Ensure we have a user to sign out
    if (!Courier.shared.isUserSignedIn) {
        client?.options?.log("No user signed into Courier. A user must be signed in on order to sign out.")
        client = null
        return@withContext
    }

    client?.options?.log("Signing user out")
    client = null

    deletePushTokens()
    closeInbox()

    // Clear the user
    // Must be called after tokens are deleted
    UserManager.removeCredentials(Courier.shared.context)

    notifyListeners()

}

fun Courier.addAuthenticationListener(onChange: (String?) -> Unit): CourierAuthenticationListener {
    val listener = CourierAuthenticationListener(onChange)
    authListeners.add(listener)
    return listener
}

fun Courier.removeAuthenticationListener(listener: CourierAuthenticationListener) {
    authListeners.removeAll { it == listener }
}

private fun Courier.notifyListeners() {
    authListeners.forEach { it.onChange(Courier.shared.userId) }
}

/**
 * Getters
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

/**
 * Traditional Callbacks
 */

fun Courier.signIn(userId: String, tenantId: String?, accessToken: String, clientKey: String?, showLogs: Boolean = BuildConfig.DEBUG, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        signIn(userId, tenantId, accessToken, clientKey, showLogs)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

fun Courier.signOut(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        signOut()
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}