package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.courier.android.Env
import com.courier.android.ExampleServer
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxClientTests {

    private suspend fun sendMessage(): String {
        return ExampleServer.sendTest(
            authKey = Env.COURIER_AUTH_KEY,
            userId = Env.COURIER_USER_ID,
            channel = "inbox"
        )
    }

    @Test
    fun getMessages() = runBlocking {

        val client = ClientBuilder.build()

        val limit = 24

        val res = client.inbox.getMessages(
            paginationLimit = limit,
            startCursor = null,
        )

        assertTrue(res.data?.messages?.nodes?.size!! <= limit)

    }

    @Test
    fun getUnreadMessageCount() = runBlocking {

        val client = ClientBuilder.build()

        val count = client.inbox.getUnreadMessageCount()

        assertTrue(count >= 0)

    }

    @Test
    fun trackClick() = runBlocking {

        // TODO: Listener

//        val messageId = sendMessage()
//
//        val client = ClientBuilder.build()
//
//        val count = client.inbox.trackClick()
//
//        assertTrue(count >= 0)

    }

    @Test
    fun trackOpen() = runBlocking {

        val messageId = sendMessage()

        val client = ClientBuilder.build()

        client.inbox.trackOpened(
            messageId = messageId,
        )

    }

    @Test
    fun trackRead() = runBlocking {

        val messageId = sendMessage()

        val client = ClientBuilder.build()

        client.inbox.trackRead(
            messageId = messageId,
        )

    }

    @Test
    fun trackUnread() = runBlocking {

        val messageId = sendMessage()

        val client = ClientBuilder.build()

        client.inbox.trackUnread(
            messageId = messageId,
        )

    }

    @Test
    fun trackArchive() = runBlocking {

        val messageId = sendMessage()

        val client = ClientBuilder.build()

        client.inbox.trackArchive(
            messageId = messageId,
        )

    }

    @Test
    fun trackReadAll() = runBlocking {

        val client = ClientBuilder.build()

        client.inbox.trackAllRead()

    }

}