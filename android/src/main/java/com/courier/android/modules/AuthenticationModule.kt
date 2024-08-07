package com.courier.android.modules

import android.content.Context
import com.courier.android.BuildConfig
import com.courier.android.Courier
import com.courier.android.Courier.Companion.registerLifecycleCallbacks
import com.courier.android.client.CourierClient
import com.courier.android.managers.UserManager
import com.courier.android.models.CourierAuthenticationListener
import com.courier.android.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Function to set the current credentials for the user and their access token
 * You should consider using this in areas where you update your local user's state
 */
suspend fun Courier.signIn(context: Context, userId: String, tenantId: String? = null, accessToken: String, clientKey: String? = null, showLogs: Boolean = BuildConfig.DEBUG) = withContext(Dispatchers.IO) {

    Courier.shared.registerLifecycleCallbacks(context)

    // Sign user out if needed
    if (Courier.shared.isUserSignedIn(context)) {
        Courier.shared.signOut(context)
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

    client?.log("Signing user in")
    client?.log("User Id: $userId")
    client?.log("Access Token: $accessToken")
    client?.log("Client Key: $clientKey")

    // Set the current user
    UserManager.setCredentials(
        context = context,
        userId = userId,
        accessToken = accessToken,
        clientKey = clientKey,
        tenantId = tenantId,
    )

    putPushTokens(context)
    refreshInbox()
    notifyListeners(context)

}

/**
 * Function that clears the current user id and access token
 * You should call this when your user signs out
 * It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
 */
suspend fun Courier.signOut(context: Context) = withContext(Dispatchers.IO) {

    // Ensure we have a user to sign out
    if (!Courier.shared.isUserSignedIn(context)) {
        client?.log("No user signed into Courier. A user must be signed in on order to sign out.")
        client = null
        return@withContext
    }

    client?.log("Signing user out")
    client = null

    deletePushTokens(context)
    closeInbox()

    // Clear the user
    // Must be called after tokens are deleted
    UserManager.removeCredentials(context)

    notifyListeners(context)

}

fun Courier.addAuthenticationListener(onChange: (String?) -> Unit): CourierAuthenticationListener {
    val listener = CourierAuthenticationListener(onChange)
    authListeners.add(listener)
    return listener
}

fun Courier.removeAuthenticationListener(listener: CourierAuthenticationListener) {
    authListeners.removeAll { it == listener }
}

private fun Courier.notifyListeners(context: Context) {
    authListeners.forEach { it.onChange(Courier.shared.getUserId(context)) }
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
internal fun Courier.getAccessToken(context: Context): String? = UserManager.getAccessToken(context)

/**
 * A read only value set to the current user id
 */
fun Courier.getUserId(context: Context): String? = UserManager.getUserId(context)

/**
 * A read only value set to the current user client key
 * https://app.courier.com/channels/courier
 */
internal fun Courier.getClientKey(context: Context): String? = UserManager.getClientKey(context)

/**
 * Token needed to authenticate with JWTs for GraphQL requests
 */
internal fun Courier.getJwt(context: Context): String? = getClientKey(context)?.let { null } ?: getAccessToken(context)

/**
 * A read only value set to the current tenant id
 */
fun Courier.getTenantId(context: Context) = UserManager.getTenantId(context)

/**
 * Determine user state
 */
fun Courier.isUserSignedIn(context: Context) = UserManager.getUserId(context) != null && getAccessToken(context) != null

/**
 * Traditional Callbacks
 */

fun Courier.signIn(context: Context, userId: String, tenantId: String?, accessToken: String, clientKey: String?, showLogs: Boolean = BuildConfig.DEBUG, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        signIn(context, userId, tenantId, accessToken, clientKey, showLogs)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

fun Courier.signOut(context: Context, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        signOut(context)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}