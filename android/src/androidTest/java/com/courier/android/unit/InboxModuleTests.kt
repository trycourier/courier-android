package com.courier.android.unit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.modules.InboxModule
import com.courier.android.ui.inbox.InboxMessageFeed
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class InboxModuleTests {

    private fun newMessage(): InboxMessage {
        val id = UUID.randomUUID().toString()
        return InboxMessage(messageId = id)
    }

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val inboxModule: InboxModule
        get() = Courier.shared.inboxModule

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    @After
    fun tearDown() = runBlocking {
        inboxModule.dispose()
    }

    @Test
    fun testListenerRegistration() = runBlocking {
        val listener = CourierInboxListener()
        inboxModule.addListener(listener)
        val listeners = inboxModule.inboxListeners
        assertEquals("Total count should increase by 1.", 1, listeners.size)
    }

    @Test
    fun testReloadData() = runBlocking {
        val initialMessage = newMessage()
        val initialData = InboxMessageSet(mutableListOf(initialMessage), 1)

        val dataStore = inboxModule.dataStore
        dataStore.updateDataSet(initialData, InboxMessageFeed.FEED)
        assertEquals(1, dataStore.feed.messages.size)

        val newData = InboxMessageSet(mutableListOf())
        dataStore.updateDataSet(newData, InboxMessageFeed.FEED)
        assertEquals(0, dataStore.feed.messages.size)
    }

    @Test
    fun testReadMessage() = runBlocking {
        val initialMessage = newMessage()
        val initialData = InboxMessageSet(mutableListOf(initialMessage), 1)

        val dataStore = inboxModule.dataStore
        dataStore.updateDataSet(initialData, InboxMessageFeed.FEED)
        dataStore.updateUnreadCount(1)
        assertEquals(initialMessage.messageId, dataStore.feed.messages.first().messageId)
        assertEquals(1, dataStore.unreadCount)

        dataStore.readMessage(initialMessage, InboxMessageFeed.FEED, null)
        assertEquals(true, dataStore.feed.messages.first().isRead)
        assertEquals(0, dataStore.unreadCount)
    }

    @Test
    fun testAddMessage() = runBlocking {
        val dataStore = inboxModule.dataStore
        dataStore.updateDataSet(InboxMessageSet(), InboxMessageFeed.FEED)
        assertEquals(true, dataStore.feed.messages.isEmpty())

        val newMessage = newMessage()
        dataStore.addMessage(newMessage, 0, InboxMessageFeed.FEED)

        assertEquals(newMessage.messageId, dataStore.feed.messages.first().messageId)
        assertEquals(1, dataStore.unreadCount)
    }

    @Test
    fun testConcurrentAddMessages() = runBlocking {
        val dataStore = inboxModule.dataStore
        dataStore.updateDataSet(InboxMessageSet(), InboxMessageFeed.FEED)

        val task1 = async {
            repeat(10) {
                val message = InboxMessage("msg_$it")
                dataStore.addMessage(message, 999, InboxMessageFeed.FEED)
            }
        }

        val task2 = async {
            repeat(10) {
                val message = InboxMessage("msg_${it + 10}")
                dataStore.addMessage(message, 999, InboxMessageFeed.FEED)
            }
        }

        task1.await()
        task2.await()

        assertEquals(20, dataStore.feed.messages.size)
    }

    @Test
    fun testConcurrentReading() = runBlocking {
        val dataStore = inboxModule.dataStore
        val unreadCount = 3
        val initialMessages = MutableList(unreadCount) { newMessage().apply { setUnread() } }
        val initialData = InboxMessageSet(initialMessages)
        dataStore.updateDataSet(initialData, InboxMessageFeed.FEED)
        dataStore.updateUnreadCount(unreadCount)

        val tasks = initialMessages.mapIndexed { index, message ->
            async {
                delay((10..100).random().toLong())
                dataStore.readMessage(message, InboxMessageFeed.FEED, null)
            }
        }

        tasks.forEach { it.await() }

        assertEquals(0, dataStore.unreadCount)
        dataStore.feed.messages.forEach { assertEquals(true, it.isRead) }
    }

}