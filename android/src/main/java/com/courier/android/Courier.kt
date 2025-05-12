package com.courier.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierAgent
import com.courier.android.models.CourierAuthenticationListener
import com.courier.android.models.CourierException
import com.courier.android.modules.InboxModule
import com.courier.android.modules.linkInbox
import com.courier.android.modules.refreshFcmToken
import com.courier.android.modules.unlinkInbox
import com.courier.android.utils.NotificationEventBus
import com.courier.android.utils.warn
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

class Courier private constructor(val context: Context) : Application.ActivityLifecycleCallbacks {

    companion object {

        // Core
        private const val VERSION = "5.2.6"
        var agent: CourierAgent = CourierAgent.NativeAndroid(VERSION)

        // Inbox
        private const val SOCKET_CLEANUP_DURATION = 60_000L * 10 // 10 minutes

        // Push
        internal const val COURIER_PENDING_NOTIFICATION_KEY = "courier_pending_notification_key"

        // Eventing
        val eventBus by lazy { NotificationEventBus() }

        // Async
        private val COURIER_COROUTINE_CONTEXT by lazy { Job() }
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
                mInstance?.refreshFcmToken()
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

    // Inbox
    internal val inboxModule = InboxModule(this)

    // Client API
    var client: CourierClient? = null
        internal set

    // Authentication
    var authListeners: MutableList<CourierAuthenticationListener> = mutableListOf()
        private set

    // Firebase
    internal val isFirebaseInitialized get() = FirebaseApp.getApps(context).isNotEmpty()

    // Push
    internal var tokens: MutableMap<String, String> = mutableMapOf()

    // Stores a local copy of the fcmToken
    internal var fcmToken: String? = null

    // We'll store the Job here, so we can cancel/renew it as needed
    private var inactivityTimerJob: Job? = null

    // Closes the inbox websocket
    private fun dispose() {
        unlinkInbox()
    }

    // Lifecycle
    override fun onActivityStarted(activity: Activity) {
        // Cancel the inactivity timer if the user comes back
        inactivityTimerJob?.cancel()
        inactivityTimerJob = null

        // Link inbox if possible
        coroutineScope.launch {
            linkInbox()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        inactivityTimerJob = coroutineScope.launch {
            delay(SOCKET_CLEANUP_DURATION)
            dispose()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    // UI debug options
    /**
     * This simplifies UI testing by providing
     * used fonts and colors in UI element tag
     */
    var isUITestsActive: Boolean = false

}