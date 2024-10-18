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
import com.courier.android.models.CourierInboxData
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.modules.InboxMutationHandler
import com.courier.android.modules.archiveMessage
import com.courier.android.modules.clickMessage
import com.courier.android.modules.linkInbox
import com.courier.android.modules.notifyError
import com.courier.android.modules.notifyInboxUpdated
import com.courier.android.modules.notifyLoading
import com.courier.android.modules.notifyMessageAdded
import com.courier.android.modules.notifyMessageRemoved
import com.courier.android.modules.notifyMessageUpdated
import com.courier.android.modules.notifyPageAdded
import com.courier.android.modules.notifyUnreadCountChange
import com.courier.android.modules.openMessage
import com.courier.android.modules.readAllInboxMessages
import com.courier.android.modules.readMessage
import com.courier.android.modules.refreshFcmToken
import com.courier.android.modules.unlinkInbox
import com.courier.android.modules.unreadMessage
import com.courier.android.socket.InboxSocket
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.android.utils.NotificationEventBus
import com.courier.android.utils.log
import com.courier.android.utils.warn
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class Courier private constructor(val context: Context) : Application.ActivityLifecycleCallbacks, InboxMutationHandler {

    companion object {

        // Core
        private const val VERSION = "4.6.0"
        var agent: CourierAgent = CourierAgent.NativeAndroid(VERSION)

        // Push
        internal const val COURIER_PENDING_NOTIFICATION_KEY = "courier_pending_notification_key"

        // Eventing
        internal val eventBus by lazy { NotificationEventBus() }

        // Async
        private val COURIER_COROUTINE_CONTEXT by lazy { Job() }
        internal val coroutineScope = CoroutineScope(COURIER_COROUTINE_CONTEXT)

        // Inbox
        const val DEFAULT_PAGINATION_LIMIT = 32
        const val DEFAULT_MAX_PAGINATION_LIMIT = 100
        const val DEFAULT_MIN_PAGINATION_LIMIT = 1

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

    // Client API
    var client: CourierClient? = null
        internal set

    // Authentication
    var authListeners: MutableList<CourierAuthenticationListener> = mutableListOf()
        private set

    // Inbox
    internal var isPagingInbox = false
    internal var paginationLimit = DEFAULT_PAGINATION_LIMIT
    internal var courierInboxData: CourierInboxData? = null
    internal var inboxListeners: MutableList<CourierInboxListener> = mutableListOf()
    internal val inboxMutationHandler: InboxMutationHandler by lazy { this }
    internal var dataPipe: Deferred<CourierInboxData?>? = null

    // Firebase
    internal val isFirebaseInitialized get() = FirebaseApp.getApps(context).isNotEmpty()

    // Push
    internal var tokens: MutableMap<String, String> = mutableMapOf()

    // Stores a local copy of the fcmToken
    internal var fcmToken: String? = null

    // Lifecycle
    override fun onActivityStarted(activity: Activity) {
        linkInbox()
    }

    override fun onActivityStopped(activity: Activity) {
        unlinkInbox()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    // Inbox Mutations
    override suspend fun onInboxReload(isRefresh: Boolean) {
        notifyLoading(isRefresh)
    }

    override suspend fun onInboxError(error: Exception) {
        notifyError(error)
        onUnreadCountChange(0)
    }

    override suspend fun onInboxUpdated(inbox: CourierInboxData) {
        this.courierInboxData = inbox
        notifyInboxUpdated(inbox)
        onUnreadCountChange(inbox.unreadCount)
    }

    override suspend fun onInboxReset(inbox: CourierInboxData, error: Throwable) {
        this.courierInboxData = inbox
        notifyInboxUpdated(inbox)
    }

    override suspend fun onUnreadCountChange(count: Int) {
        notifyUnreadCountChange(count)
    }

    override suspend fun onInboxKilled() {
        Courier.shared.client?.log("Inbox killed")
    }

    override suspend fun onInboxMessageReceived(message: InboxMessage) {
        val index = 0
        val feed = if (message.isArchived) InboxMessageFeed.ARCHIVE else InboxMessageFeed.FEED
        val unreadCount = this.courierInboxData?.addNewMessage(feed, index, message)
        onInboxItemAdded(index, feed, message)
        onUnreadCountChange(count = unreadCount ?: 0)
    }

    override suspend fun onInboxItemAdded(index: Int, feed: InboxMessageFeed, message: InboxMessage) {
        notifyMessageAdded(feed, index, message)
    }

    override suspend fun onInboxItemRemove(index: Int, feed: InboxMessageFeed, message: InboxMessage) {
        notifyMessageRemoved(feed, index, message)
    }

    override suspend fun onInboxItemUpdated(index: Int, feed: InboxMessageFeed, message: InboxMessage) {
        notifyMessageUpdated(feed, index, message)
    }

    override suspend fun onInboxPageFetched(feed: InboxMessageFeed, messageSet: InboxMessageSet) {
        this.courierInboxData?.addPage(feed, messageSet)
        notifyPageAdded(feed, messageSet)
    }

    override suspend fun onInboxEventReceived(event: InboxSocket.MessageEvent) {
        try {
            when (event.event) {
                InboxSocket.EventType.MARK_ALL_READ -> {
                    Courier.shared.readAllInboxMessages()
                }
                InboxSocket.EventType.READ -> {
                    event.messageId?.let { messageId ->
                        Courier.shared.readMessage(messageId)
                    }
                }
                InboxSocket.EventType.UNREAD -> {
                    event.messageId?.let { messageId ->
                        Courier.shared.unreadMessage(messageId)
                    }
                }
                InboxSocket.EventType.OPENED -> {
                    event.messageId?.let { messageId ->
                        Courier.shared.openMessage(messageId)
                    }
                }
                InboxSocket.EventType.UNOPENED -> {
                    // No action needed for unopened
                }
                InboxSocket.EventType.ARCHIVE -> {
                    event.messageId?.let { messageId ->
                        Courier.shared.archiveMessage(messageId)
                    }
                }
                InboxSocket.EventType.UNARCHIVE -> {
                    // No action needed for unarchive
                }
                InboxSocket.EventType.CLICK -> {
                    event.messageId?.let { messageId ->
                        Courier.shared.clickMessage(messageId)
                    }
                }
                InboxSocket.EventType.UNCLICK -> {
                    // No action needed for unclick
                }
            }
        } catch (e: Exception) {
            Courier.shared.client?.log(e.localizedMessage ?: "Error occurred")
        }
    }

}