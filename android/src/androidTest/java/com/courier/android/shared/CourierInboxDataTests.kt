package com.courier.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.Env
import com.courier.android.ExampleServer
import com.courier.android.UserBuilder
import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxMessage
import com.courier.android.models.markAsArchived
import com.courier.android.models.markAsClicked
import com.courier.android.models.markAsOpened
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.modules.InboxModule
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.archiveMessage
import com.courier.android.modules.clickMessage
import com.courier.android.modules.fetchNextInboxPage
import com.courier.android.modules.inboxPaginationLimit
import com.courier.android.modules.openMessage
import com.courier.android.modules.readMessage
import com.courier.android.modules.unreadMessage
import com.courier.android.ui.inbox.InboxMessageEvent
import com.courier.android.ui.inbox.InboxMessageFeed
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@RunWith(AndroidJUnit4::class)
class CourierInboxDataTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    private suspend fun sendInboxMessageWithConfirmation(
        userId: String,
        tenantId: String? = null
    ): Pair<InboxMessage, CourierInboxListener> = suspendCancellableCoroutine { continuation ->

        val isCompleted = AtomicBoolean(false)
        var messageId: String? = null
        var listener: CourierInboxListener? = null

        // Launch the background task
        CoroutineScope(Dispatchers.IO).launch {

            // Step 1: Set up the inbox listener
            listener = Courier.shared.addInboxListener(
                onMessageEvent = { message, index, feed, event ->
                    CoroutineScope(Dispatchers.Default).launch {
                        if (
                            event == InboxMessageEvent.ADDED &&
                            message.messageId == messageId &&
                            isCompleted.compareAndSet(false, true)
                        ) {
                            messageId = null
                            continuation.resume(message to listener!!)
                        }
                    }
                }
            )

            try {
                // Step 2: Send the test message
                var newMessageId = ExampleServer.sendTest(
                    authKey = Env.COURIER_AUTH_KEY,
                    userId = userId,
                    tenantId = tenantId,
                    channel = "inbox"
                )

                if (tenantId != null) {
                    newMessageId += ":$userId" // Hack due to backend quirks 💔
                }

                messageId = newMessageId
                println("New message sent: $messageId")

                // Step 3: Timeout after 30 seconds if message not received
                delay(30_000L)

                if (isCompleted.compareAndSet(false, true)) {
                    continuation.resumeWithException(CourierException.inboxNotInitialized)
                }

            } catch (e: Exception) {
                if (isCompleted.compareAndSet(false, true)) {
                    continuation.resumeWithException(e)
                }
            }
        }

        // Optional: handle cancellation
        continuation.invokeOnCancellation {
            if (isCompleted.compareAndSet(false, true)) {
                listener?.remove()
            }
        }
    }

    private suspend fun sendMessage(userId: String = Env.COURIER_USER_ID): String {
        return ExampleServer.sendTest(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId,
            channel = "inbox"
        )
    }

    private fun getMessageFromDataStore(messageId: String, feedType: InboxMessageFeed): InboxMessage {
        val dataStore = Courier.shared.inboxModule.dataStore
        return dataStore.getMessageById(feedType = feedType, messageId = messageId)!!
    }

    @Test
    fun openMessage() = runBlocking {

        val userId = UserBuilder.authenticate()

        val (message, listener) = sendInboxMessageWithConfirmation(userId)

        val state1 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state1.isOpened, false)

        Courier.shared.openMessage(message.messageId)

        val state2 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state2.isOpened, true)

        listener.remove()

    }

    @Test
    fun readMessage() = runBlocking {
        val userId = UserBuilder.authenticate()

        val (message, listener) = sendInboxMessageWithConfirmation(userId)

        val state1 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state1.isRead, false)

        Courier.shared.readMessage(message.messageId)

        val state2 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state2.isRead, true)

        listener.remove()
    }

    @Test
    fun unreadMessage() = runBlocking {
        val userId = UserBuilder.authenticate()

        val (message, listener) = sendInboxMessageWithConfirmation(userId)

        Courier.shared.readMessage(message.messageId) // Ensure it's read first
        val state1 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state1.isRead, true)

        Courier.shared.unreadMessage(message.messageId)

        val state2 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state2.isRead, false)

        listener.remove()
    }


    @Test
    fun clickMessage() = runBlocking {
        val userId = UserBuilder.authenticate()

        val (message, listener) = sendInboxMessageWithConfirmation(userId)

        Courier.shared.clickMessage(message.messageId)

        listener.remove()
    }

    @Test
    fun archiveMessage() = runBlocking {
        val userId = UserBuilder.authenticate()

        val (message, listener) = sendInboxMessageWithConfirmation(userId)

        val state1 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state1.isArchived, false)

        Courier.shared.archiveMessage(message.messageId)

        val state2 = getMessageFromDataStore(message.messageId, InboxMessageFeed.ARCHIVE)
        assertEquals(state2.isArchived, true)

        listener.remove()
    }

    @Test
    fun shortcuts() = runBlocking {

        val userId = UserBuilder.authenticate()

        val (message, listener) = sendInboxMessageWithConfirmation(userId)

        message.markAsOpened()
        message.markAsUnread()
        message.markAsRead()
        message.markAsClicked()
        message.markAsArchived()

        listener.remove()
    }

    @Test
    fun tenantSend() = runBlocking {

        val userId = "t1-user"
        val tenantId = "t1"

        UserBuilder.authenticate(userId = userId, tenantId = tenantId)

        val (message, listener) = sendInboxMessageWithConfirmation(userId, tenantId)

        val state1 = getMessageFromDataStore(message.messageId, InboxMessageFeed.FEED)
        assertEquals(state1.isArchived, false)

        listener.remove()
    }

    @Test
    fun multipleListeners() = runBlocking {

        UserBuilder.authenticate()

        var hold1 = true
        var hold2 = true

        val listener1 = Courier.shared.addInboxListener(
            onMessagesChanged = { messages, canPaginate, feed ->
                hold1 = false
            }
        )

        val listener2 = Courier.shared.addInboxListener(
            onMessagesChanged = { messages, canPaginate, feed ->
                hold2 = false
            }
        )

        while (hold1 && hold2) {
            // Wait for registration
        }

        listener1.remove()
        listener2.remove()

    }

    @Test
    fun pagination() = runBlocking {

        val userId = UUID.randomUUID().toString()
        UserBuilder.authenticate(userId)

        Courier.shared.inboxPaginationLimit = 10000
        assertEquals(Courier.shared.inboxPaginationLimit, InboxModule.Pagination.MAX.value)

        Courier.shared.inboxPaginationLimit = -10000
        assertEquals(Courier.shared.inboxPaginationLimit, InboxModule.Pagination.MIN.value)

        Courier.shared.inboxPaginationLimit = 1
        assertEquals(Courier.shared.inboxPaginationLimit, 1)

        val sendCount = 5

        val listener = Courier.shared.addInboxListener(
            onPageAdded = { messages, canPaginate, isFirstPage, feed ->
                Courier.coroutineScope.launch {
                    if (canPaginate) {
                        Courier.shared.fetchNextInboxPage(InboxMessageFeed.FEED)
                    }
                }
            }
        )

        // Send some messages to the user
        for (i in 1..sendCount) {
            sendMessage(userId = userId)
        }

        val messages = Courier.shared.inboxModule.dataStore.feed.messages

        // Hold until the listener paginates to fetch the messages
        while (messages.size != sendCount) {
            // Wait for count
        }

        listener.remove()

        assertEquals(messages.size, sendCount)

    }

}