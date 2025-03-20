package com.courier.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.Env
import com.courier.android.ExampleServer
import com.courier.android.UserBuilder
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
import com.courier.android.ui.inbox.InboxMessageFeed
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CourierInboxDataTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    private suspend fun sendMessage(userId: String = Env.COURIER_USER_ID): String {
        return ExampleServer.sendTest(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId,
            channel = "inbox"
        )
    }

    @Test
    fun openMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000)

        Courier.shared.openMessage(messageId)

    }

    @Test
    fun readMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000)

        Courier.shared.readMessage(messageId)

    }

    @Test
    fun unreadMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000)

        Courier.shared.unreadMessage(messageId)

    }

    @Test
    fun clickMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000)

        // Will succeed by default
        Courier.shared.clickMessage(messageId)

    }

    @Test
    fun archiveMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000)

        Courier.shared.archiveMessage(messageId)

    }

    @Test
    fun shortcuts() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000)

        val message = InboxMessage(messageId = messageId)

        message.markAsOpened()
        message.markAsUnread()
        message.markAsRead()
        message.markAsClicked()
        message.markAsArchived()

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

        UserBuilder.authenticate()

        Courier.shared.inboxPaginationLimit = 10000
        assertEquals(Courier.shared.inboxPaginationLimit, InboxModule.Pagination.MAX)

        Courier.shared.inboxPaginationLimit = -10000
        assertEquals(Courier.shared.inboxPaginationLimit, InboxModule.Pagination.MIN)

        Courier.shared.inboxPaginationLimit = 1
        assertEquals(Courier.shared.inboxPaginationLimit, 1)

        val sendCount = 5

        val listener = Courier.shared.addInboxListener(
            onMessagesChanged = { messages, canPaginate, feed ->
                Courier.coroutineScope.launch {
                    if (canPaginate) {
                        Courier.shared.fetchNextInboxPage(InboxMessageFeed.FEED)
                    }
                }
            }
        )

        // Send some messages to the user
        for (i in 1..sendCount) {
            sendMessage()
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