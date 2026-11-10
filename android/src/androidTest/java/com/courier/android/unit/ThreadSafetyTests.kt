package com.courier.android.unit

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.ExampleServer
import com.courier.android.models.CourierAuthenticationListener
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.models.remove
import com.courier.android.modules.InboxModule
import com.courier.android.modules.addAuthenticationListener
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.android.modules.refreshInbox
import com.courier.android.ui.inbox.InboxMessageEvent
import com.courier.android.ui.inbox.InboxMessageFeed
import com.courier.app.Env
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Listeners, tokens and inbox data are touched from several threads at once in real apps:
// socket frames on IO, user actions and UI on main, sign in on IO, FCM on its own thread.
@RunWith(AndroidJUnit4::class)
class ThreadSafetyTests {

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
    fun authListenerCanAddAListenerWhileBeingNotified() = runBlocking {
        val calledOnMain = mutableListOf<Boolean>()
        var added: CourierAuthenticationListener? = null

        val listener = Courier.shared.addAuthenticationListener {
            calledOnMain.add(Looper.myLooper() == Looper.getMainLooper())
            if (added == null) {
                added = Courier.shared.addAuthenticationListener {}
            }
        }

        // Token requests fail with these credentials, which sign in logs and moves past,
        // so this only exercises the listener notifications
        Courier.shared.signIn(userId = "thread-safety-${UUID.randomUUID()}", accessToken = "test-token")
        Courier.shared.signOut()

        listener.remove()
        added?.remove()

        assertTrue("The listener was never called", calledOnMain.isNotEmpty())
        assertTrue("Auth listeners should be called on main", calledOnMain.all { it })
    }

    @Test
    fun inboxListenerCanRemoveItselfWhileBeingNotified() = runBlocking {
        var secondCalls = 0
        lateinit var first: CourierInboxListener

        first = CourierInboxListener(onUnreadCountChanged = { inboxModule.removeListener(first) })
        val second = CourierInboxListener(onUnreadCountChanged = { secondCalls++ })

        inboxModule.addListener(first)
        inboxModule.addListener(second)

        inboxModule.dataStore.updateUnreadCount(3)

        assertEquals(1, secondCalls)
        assertEquals(listOf(second), inboxModule.inboxListeners.toList())
    }

    @Test
    fun concurrentMessageAddsAreAllKept() = runBlocking {
        val dataStore = inboxModule.dataStore
        dataStore.updateDataSet(InboxMessageSet(), InboxMessageFeed.FEED)
        dataStore.updateUnreadCount(0)

        val count = 1500

        (1..count).map {
            launch(Dispatchers.Default) {
                val message = InboxMessage(messageId = UUID.randomUUID().toString())
                dataStore.addMessage(message, 0, InboxMessageFeed.FEED)
            }
        }.joinAll()

        assertEquals(count, dataStore.feed.messages.size)
        assertEquals(count, dataStore.feed.totalCount)
        assertEquals(count, dataStore.unreadCount)
    }

    @Test
    fun tokensCanChangeWhileBeingRead() = runBlocking {
        val tokens = Courier.shared.tokens
        val count = 2000

        val writer = launch(Dispatchers.Default) {
            repeat(count) { tokens["race-$it"] = "token-$it" }
        }
        val reader = launch(Dispatchers.Default) {
            while (writer.isActive) {
                tokens.forEach { _ -> }
            }
        }
        joinAll(writer, reader)

        val keys = tokens.keys.filter { it.startsWith("race-") }
        assertEquals(count, keys.size)
        keys.forEach { tokens.remove(it) }
    }

    @Test
    fun overlappingInboxLoadsDeliverEachMessageOnce() = runBlocking {
        val userId = "thread-safety-${UUID.randomUUID()}"
        Courier.shared.signOut()
        Courier.shared.signIn(
            userId = userId,
            accessToken = ExampleServer.generateJWT(authKey = Env.COURIER_AUTH_KEY, userId = userId)
        )

        val added = ConcurrentHashMap<String, Int>()
        val listener = Courier.shared.addInboxListener(
            onMessageEvent = { message, _, feed, event ->
                if (event == InboxMessageEvent.ADDED && feed == InboxMessageFeed.FEED) {
                    added.merge(message.messageId, 1, Int::plus)
                }
            }
        )

        // Loads that overlap, like the app returning to the foreground during a pull to refresh
        (1..6).map { index ->
            launch(if (index % 2 == 0) Dispatchers.Default else Dispatchers.IO) {
                Courier.shared.refreshInbox()
            }
        }.joinAll()

        // Give the socket time to subscribe
        delay(3_000)

        val messageId = ExampleServer.sendTest(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId,
            channel = "inbox"
        )

        withTimeout(60_000) {
            while (!added.containsKey(messageId)) delay(250)
        }

        // A second socket would deliver the same message again in this window
        delay(5_000)

        listener.remove()
        Courier.shared.signOut()

        assertEquals(1, added[messageId])
    }

}
