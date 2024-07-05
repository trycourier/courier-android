package com.courier.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.courier.android.client.CourierClient
import com.courier.android.managers.UserManager
import com.courier.android.models.CourierAgent
import com.courier.android.models.CourierAuthenticationListener
import com.courier.android.models.CourierException
import com.courier.android.modules.isUserSignedIn
import com.courier.android.modules.userId
import com.courier.android.utils.NotificationEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**

     ,gggg,
   ,88"""Y8b,
  d8"     `Y8
 d8'   8b  d8                                      gg
,8I    "Y88P'                                      ""
I8'             ,ggggg,    gg      gg   ,gggggg,   gg    ,ggg,    ,gggggg,
d8             dP"  "Y8ggg I8      8I   dP""""8I   88   i8" "8i   dP""""8I
Y8,           i8'    ,8I   I8,    ,8I  ,8'    8I   88   I8, ,8I  ,8'    8I
`Yba,,_____, ,d8,   ,d8'  ,d8b,  ,d8b,,dP     Y8,_,88,_ `YbadP' ,dP     Y8,
  `"Y8888888 P"Y8888P"    8P'"Y88P"`Y88P      `Y88P""Y8888P"Y8888P      `Y8

===========================================================================

 More about Courier: https://courier.com
 Android Documentation: https://github.com/trycourier/courier-android

===========================================================================

*/

class Courier private constructor(internal val context: Context) : Application.ActivityLifecycleCallbacks {

    internal var client: CourierClient? = null

    /**
     * Modules
     */
//    internal val auth by lazy { CoreAuth() }
//    internal val push by lazy { CorePush() }
//    internal val inbox by lazy { CoreInbox() }
//    internal val preferences by lazy { CorePreferences() }
//    internal val brand by lazy { CoreBrand() }
//    internal val messaging by lazy { CoreMessaging() }

    companion object {

        var USER_AGENT = CourierAgent.NATIVE_ANDROID
        internal const val VERSION = "3.5.10"
        internal const val TAG = "Courier SDK"
        internal const val COURIER_PENDING_NOTIFICATION_KEY = "courier_pending_notification_key"
        internal val eventBus by lazy { NotificationEventBus() }
        internal val COURIER_COROUTINE_CONTEXT by lazy { Job() }
        internal val coroutineScope = CoroutineScope(COURIER_COROUTINE_CONTEXT)

        // This will not create a memory leak
        // Please call Courier.initialize(context) before using Courier.shared
        @SuppressLint("StaticFieldLeak")
        private var mInstance: Courier? = null
        val shared: Courier
            get() {
                mInstance?.let { return it }
                throw CourierException.initializationError
            }

        /**
         * Initializes the SDK with a static reference to a Courier singleton
         * This function must be called before you can use the Courier.shared value
         * Courier.shared is required for nearly all features of the SDK
         */
        fun initialize(context: Context) {

            // Create the new instance if needed
            if (mInstance == null) {
                mInstance = Courier(context)
            }

            // Register lifecycle callbacks
            mInstance?.registerLifecycleCallbacks()

            // Get the current fcmToken if possible
            coroutineScope.launch(Dispatchers.IO) {
//                mInstance?.push?.refreshFcmToken()
            }

        }

        private fun Courier.registerLifecycleCallbacks() {
            when (context) {
                is Application -> {
                    context.unregisterActivityLifecycleCallbacks(this)
                    context.registerActivityLifecycleCallbacks(this)
                }
                is Activity -> {

                    // Only available in 29+
                    // Fallback to the Application
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.unregisterActivityLifecycleCallbacks(this)
                        context.registerActivityLifecycleCallbacks(this)
                    } else {
                        context.application.unregisterActivityLifecycleCallbacks(this)
                        context.application.registerActivityLifecycleCallbacks(this)
                    }

                }
                else -> {
                    client?.warn("Initialization context does not support lifecycle callbacks. Please call Courier.initialize(context) with an Activity or Application context.")
                }
            }
        }

    }

    private var authListeners: MutableList<CourierAuthenticationListener> = mutableListOf()

    /**
     * Function to set the current credentials for the user and their access token
     * You should consider using this in areas where you update your local user's state
     */
    suspend fun signIn(userId: String, tenantId: String?, accessToken: String, clientKey: String?, showLogs: Boolean = BuildConfig.DEBUG) = withContext(Dispatchers.IO) {

        // Sign user out if needed
        if (shared.isUserSignedIn) {
            shared.signOut()
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
            context = shared.context,
            userId = userId,
            accessToken = accessToken,
            clientKey = clientKey,
            tenantId = tenantId,
        )

//        push.putPushTokens()
//        inbox.restart()
        notifyListeners()

    }

    /**
     * Function that clears the current user id and access token
     * You should call this when your user signs out
     * It will remove the current tokens used for this user in Courier so they do not receive pushes they should not get
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {

        // Ensure we have a user to sign out
        if (!shared.isUserSignedIn) {
            client?.log("No user signed into Courier. A user must be signed in on order to sign out.")
            return@withContext
        }

        client?.log("Signing user out")

//        push.deletePushTokens()
//
//        inbox.close()

        // Clear the user
        // Must be called after tokens are deleted
        UserManager.removeCredentials(shared.context)

        notifyListeners()

    }

    internal fun addAuthChangeListener(onChange: (String?) -> Unit): CourierAuthenticationListener {
        val listener = CourierAuthenticationListener(onChange)
        authListeners.add(listener)
        return listener
    }

    internal fun removeAuthenticationListener(listener: CourierAuthenticationListener) {
        authListeners.removeAll { it == listener }
    }

    private fun notifyListeners() {
        authListeners.forEach { it.onChange(Courier.shared.userId) }
    }

    override fun onActivityStarted(activity: Activity) {
//        inbox.link()
    }

    override fun onActivityStopped(activity: Activity) {
//        inbox.unlink()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}