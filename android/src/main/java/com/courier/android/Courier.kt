package com.courier.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.courier.android.models.CourierAgent
import com.courier.android.models.CourierException
import com.courier.android.modules.*
import com.courier.android.utils.NotificationEventBus
import kotlinx.coroutines.*

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

    /**
     * Modules
     */
    internal val logging = CoreLogging()
    internal val auth by lazy { CoreAuth() }
    internal val push by lazy { CorePush() }
    internal val inbox by lazy { CoreInbox() }
    internal val preferences by lazy { CorePreferences() }
    internal val messaging by lazy { CoreMessaging() }

    companion object {

        var USER_AGENT = CourierAgent.NATIVE_ANDROID
        internal const val VERSION = "2.3.2"
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
            // This will register if the API target is 29 and higher
            if (context is Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.unregisterActivityLifecycleCallbacks(mInstance!!)
                context.registerActivityLifecycleCallbacks(mInstance!!)
            }

            // Get the current fcmToken if possible
            coroutineScope.launch(Dispatchers.IO) {
                mInstance?.push?.refreshFcmToken()
            }

        }

        /**
         * Logs to the console
         */
        fun log(data: String) = shared.logging.log(data)
        fun warn(data: String) = shared.logging.warn(data)
        fun error(data: String?) = shared.logging.error(data)

    }

    override fun onActivityStarted(activity: Activity) {
        inbox.link()
    }

    override fun onActivityStopped(activity: Activity) {
        inbox.unlink()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}