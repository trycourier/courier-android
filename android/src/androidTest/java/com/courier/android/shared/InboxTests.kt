package com.courier.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.Courier.Companion.DEFAULT_MAX_PAGINATION_LIMIT
import com.courier.android.Courier.Companion.DEFAULT_MIN_PAGINATION_LIMIT
import com.courier.android.Env
import com.courier.android.ExampleServer
import com.courier.android.UserBuilder
import com.courier.android.models.InboxMessage
import com.courier.android.models.markAsArchived
import com.courier.android.models.markAsClicked
import com.courier.android.models.markAsOpened
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.archiveMessage
import com.courier.android.modules.fetchNextPage
import com.courier.android.modules.inboxPaginationLimit
import com.courier.android.modules.openMessage
import com.courier.android.modules.readMessage
import com.courier.android.modules.unreadMessage
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxTests {

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

    private suspend fun getVerifiedInboxMessage(): InboxMessage {

        val listener = Courier.shared.addInboxListener()

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val message = Courier.shared.inboxMessages?.first()

        assertNotNull(message)
        assertEquals(message!!.messageId, messageId)

        listener.remove()

        return message

    }

    @Test
    fun openMessage() = runBlocking {

        UserBuilder.authenticate()

        val message = getVerifiedInboxMessage()

        Courier.shared.openMessage(message.messageId)

    }

    @Test
    fun readMessage() = runBlocking {

        UserBuilder.authenticate()

        val message = getVerifiedInboxMessage()

        Courier.shared.readMessage(message.messageId)

    }

    @Test
    fun unreadMessage() = runBlocking {

        UserBuilder.authenticate()

        val message = getVerifiedInboxMessage()

        Courier.shared.unreadMessage(message.messageId)

    }

    @Test
    fun clickMessage() = runBlocking {

        // TODO: Skipping for now

//        UserBuilder.authenticate()
//
//        val message = getVerifiedInboxMessage()
//
//        Courier.shared.clickMessage(message.messageId)

    }

    @Test
    fun archiveMessage() = runBlocking {

        UserBuilder.authenticate()

        val message = getVerifiedInboxMessage()

        Courier.shared.archiveMessage(message.messageId)

    }

    @Test
    fun shortcuts() = runBlocking {

        UserBuilder.authenticate()

        val message = getVerifiedInboxMessage()

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

        val listener1 = Courier.shared.addInboxListener { _, _, _, _ ->
            hold1 = false
        }

        val listener2 = Courier.shared.addInboxListener { _, _, _, _ ->
            hold2 = false
        }

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
        assertEquals(Courier.shared.inboxPaginationLimit, DEFAULT_MAX_PAGINATION_LIMIT)

        Courier.shared.inboxPaginationLimit = -10000
        assertEquals(Courier.shared.inboxPaginationLimit, DEFAULT_MIN_PAGINATION_LIMIT)

        Courier.shared.inboxPaginationLimit = 1
        assertEquals(Courier.shared.inboxPaginationLimit, 1)

        val sendCount = 5

        val listener = Courier.shared.addInboxListener { messages, _, _, canPaginate ->

            Courier.coroutineScope.launch {

                if (canPaginate) {
                    Courier.shared.fetchNextPage()
                }

            }

        }

        // Send some messages to the user
        for (i in 1..sendCount) {
            sendMessage()
        }

        val messages = Courier.shared.inboxMessages

        // Hold until the listener paginates to fetch the messages
        while (messages?.size != sendCount) {
            // Wait for count
        }

        listener.remove()

        assertEquals(messages.size, sendCount)

    }

}