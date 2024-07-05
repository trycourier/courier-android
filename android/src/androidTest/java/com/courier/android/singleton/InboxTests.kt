package com.courier.android.singleton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.Env
import com.courier.android.ExampleServer
import com.courier.android.models.markAsArchived
import com.courier.android.models.markAsClicked
import com.courier.android.models.markAsOpened
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.archiveMessage
import com.courier.android.modules.clickMessage
import com.courier.android.modules.fetchNextPage
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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class InboxTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private suspend fun sendMessage(userId: String = Env.COURIER_USER_ID): String {
        return ExampleServer.sendTest(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId,
            channel = "inbox"
        )
    }

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    @Test
    fun openMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val res = Courier.shared.client?.inbox?.getMessage(
            messageId = messageId
        )

        val message = res?.data?.message

        assertNotNull(message)

        Courier.shared.openMessage(
            messageId = messageId,
        )

        message!!.markAsOpened()

    }

    @Test
    fun readMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val res = Courier.shared.client?.inbox?.getMessage(
            messageId = messageId
        )

        val message = res?.data?.message

        assertNotNull(message)

        Courier.shared.readMessage(
            messageId = messageId,
        )

        message!!.markAsRead()

    }

    @Test
    fun unreadMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val res = Courier.shared.client?.inbox?.getMessage(
            messageId = messageId
        )

        val message = res?.data?.message

        assertNotNull(message)

        Courier.shared.unreadMessage(
            messageId = messageId,
        )

        message!!.markAsUnread()

    }

    @Test
    fun clickMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val res = Courier.shared.client?.inbox?.getMessage(
            messageId = messageId
        )

        val message = res?.data?.message

        assertNotNull(message)

        Courier.shared.clickMessage(
            messageId = messageId,
        )

        message!!.markAsClicked()

    }

    @Test
    fun archiveMessage() = runBlocking {

        UserBuilder.authenticate()

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val res = Courier.shared.client?.inbox?.getMessage(
            messageId = messageId
        )

        val message = res?.data?.message

        assertNotNull(message)

        Courier.shared.archiveMessage(
            messageId = messageId,
        )

        message!!.markAsArchived()

    }

    @Test
    fun setupInbox() = runBlocking {

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
    fun testInboxPagination() = runBlocking {

        val userId = UUID.randomUUID().toString()

        UserBuilder.authenticate(userId)

        Courier.shared.paginationLimit = 1

        sendMessage(userId)
        sendMessage(userId)
        sendMessage(userId)

        delay(5000)

        val listener = Courier.shared.addInboxListener { _, _, _, canPaginate ->

            Courier.coroutineScope.launch {

                if (canPaginate) {
                    Courier.shared.fetchNextPage()
                }

            }

        }

        val messages = Courier.shared.inboxMessages

        while (messages?.size!! < 3) {
            // Wait for count
        }

        listener.remove()

        assertEquals(messages.size, 3)

    }

}