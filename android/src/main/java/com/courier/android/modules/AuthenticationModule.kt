package com.courier.android.modules

import com.courier.android.BuildConfig
import com.courier.android.Courier
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
suspend fun Courier.signIn(userId: String, tenantId: String? = null, accessToken: String, clientKey: String? = null, apiUrls: CourierClient.ApiUrls = CourierClient.ApiUrls(), showLogs: Boolean = BuildConfig.DEBUG) = withContext(Dispatchers.IO) {

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
        apiUrls = apiUrls,
        showLogs = showLogs,
    )

    // The access token and client key are credentials, so they stay out of the logs
    client?.log("Signing user in")
    client?.log("User Id: $userId")
    client?.log("Client Key: ${if (clientKey != null) "set" else "none"}")

    // Set the current user
    UserManager.setCredentials(
        context = Courier.shared.context,
        userId = userId,
        accessToken = accessToken,
        clientKey = clientKey,
        tenantId = tenantId,
        apiUrls = apiUrls,
        showLogs = showLogs,
    )

    putPushTokens()
    restartInbox()
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
        client?.log("No user signed into Courier. A user must be signed in on order to sign out.")
        client = null
        return@withContext
    }

    client?.log("Signing user out")

    // The token deletes go through the client, so it's cleared only after them
    deletePushTokens()
    closeInbox()
    client = null

    // Clear the user
    // Must be called after tokens are deleted
    UserManager.removeCredentials(Courier.shared.context)

    notifyListeners()

}

/**
 * Credentials outlive the process but the client does not, so without this a relaunched app
 * reports a signed in user while everything that needs the client fails until signIn runs again.
 */
internal fun Courier.restoreClient(): CourierClient? {

    // Sessions saved before the urls were stored are left alone. Falling back to the default
    // urls would send a token issued in another region to the wrong API.
    val userId = UserManager.getUserId(context) ?: return null
    val accessToken = UserManager.getAccessToken(context) ?: return null
    val apiUrls = UserManager.getApiUrls(context) ?: return null
    val showLogs = UserManager.getShowLogs(context) ?: return null

    val client = CourierClient(
        jwt = accessToken,
        clientKey = UserManager.getClientKey(context),
        userId = userId,
        connectionId = UUID.randomUUID().toString(),
        tenantId = UserManager.getTenantId(context),
        apiUrls = apiUrls,
        showLogs = showLogs,
    )

    client.log("Restored signed in user: $userId")

    return client

}

fun Courier.addAuthenticationListener(onChange: (String?) -> Unit): CourierAuthenticationListener {
    val listener = CourierAuthenticationListener(onChange)
    authListeners.add(listener)
    return listener
}

fun Courier.removeAuthenticationListener(listener: CourierAuthenticationListener) {
    authListeners.removeAll { it == listener }
}

// Called on main, like inbox listeners, so apps can update their UI from the callback
private suspend fun Courier.notifyListeners() {
    val userId = Courier.shared.userId
    withContext(Dispatchers.Main) {
        authListeners.forEach { it.onChange(userId) }
    }
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

fun Courier.signIn(userId: String, tenantId: String?, accessToken: String, clientKey: String?, apiUrls: CourierClient.ApiUrls = CourierClient.ApiUrls(), showLogs: Boolean = BuildConfig.DEBUG, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.Main) {
    try {
        signIn(userId, tenantId, accessToken, clientKey, apiUrls, showLogs)
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