package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.courier.android.ClientBuilder
import com.courier.android.Env
import com.courier.android.ExampleServer
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CourierInboxDataClientTests {

    private lateinit var client: CourierClient
    private val connectionId = UUID.randomUUID().toString()

    @Before
    fun setup() = runBlocking {
        client = ClientBuilder.build(
            connectionId = connectionId
        )
    }

    private suspend fun sendMessage(userId: String = Env.COURIER_USER_ID): String {
        return ExampleServer.sendTest(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId,
            channel = "inbox"
        )
    }

    @Test
    fun getMessage() = runBlocking {

        val messageId = sendMessage()

        delay(5000) // Pipeline delay

        val message = client.inbox.getMessage(
            messageId = messageId
        )

        assertNotNull(message)

    }

    @Test
    fun getMessages() = runBlocking {

        val limit = 24

        val res = client.inbox.getMessages(
            paginationLimit = limit,
            startCursor = null,
        )

        assertTrue(res.data?.messages?.nodes?.size!! <= limit)

    }

    @Test
    fun getArchivedMessages() = runBlocking {

        // Archive a new message
        val messageId = sendMessage(userId = client.options.userId)

        delay(5000) // Pipeline delay

        client.inbox.archive(messageId)

        delay(5000) // Pipeline delay

        val limit = 24

        val res = client.inbox.getArchivedMessages(
            paginationLimit = limit,
            startCursor = null,
        )

        val message = res.data?.messages?.nodes?.firstOrNull()

        assertTrue(message?.isArchived == true)

    }

    @Test
    fun getUnreadMessageCount() = runBlocking {

        sendMessage()

        delay(5000) // Pipeline delay

        val count = client.inbox.getUnreadMessageCount()

        assertTrue(count >= 0)

    }

    @Test
    fun trackClick() = runBlocking {

        val messageId = sendMessage()

        client.inbox.click(
            messageId = messageId,
            trackingId = "example_id"
        )

    }

    @Test
    fun trackOpen() = runBlocking {

        val messageId = sendMessage()

        client.inbox.open(
            messageId = messageId,
        )

    }

    @Test
    fun trackRead() = runBlocking {

        val messageId = sendMessage()

        client.inbox.read(
            messageId = messageId,
        )

    }

    @Test
    fun trackUnread() = runBlocking {

        val messageId = sendMessage()

        client.inbox.unread(
            messageId = messageId,
        )

    }

    @Test
    fun trackArchive() = runBlocking {

        val messageId = sendMessage()

        client.inbox.archive(
            messageId = messageId,
        )

    }

    @Test
    fun trackReadAll() = runBlocking {

        client.inbox.readAll()

    }

    @Test
    fun multipleSocketsOnSingleUser() = runBlocking {

        var hold1 = true
        var hold2 = true

        // Open the first socket connection
        val client1 = ClientBuilder.build(connectionId = UUID.randomUUID().toString()).apply {

            val socket = inbox.socket

            socket.onOpen = {
                println("Socket Opened")
            }

            socket.onClose = { code, reason ->
                println("Socket closed: $code, $reason")
            }

            socket.onError = { error ->
                assertNull(error)
            }

            socket.receivedMessageEvent = { event ->
                println(event)
            }

            socket.receivedMessage = { message ->
                println(message)
                hold1 = false
            }

            socket.connect()
            socket.sendSubscribe()

        }

        // Open the second socket connection
        val client2 = ClientBuilder.build(connectionId = UUID.randomUUID().toString()).apply {

            val socket = inbox.socket

            socket.onOpen = {
                println("Socket Opened")
            }

            socket.onClose = { code, reason ->
                println("Socket closed: $code, $reason")
            }

            socket.onError = { error ->
                assertNull(error)
            }

            socket.receivedMessageEvent = { event ->
                println(event)
            }

            socket.receivedMessage = { message ->
                println(message)
                hold2 = false
            }

            socket.connect()
            socket.sendSubscribe()

        }

        val messageId = sendMessage()

        print(messageId)

        while (hold1 || hold2) {
            // Wait for the message to be received in the sockets
        }

        client1.inbox.socket.disconnect()
        client2.inbox.socket.disconnect()

    }

    @Test
    fun multipleUserConnections() = runBlocking {

        val userId1 = "user_1"
        val userId2 = "user_2"

        var hold1 = true
        var hold2 = true

        val jwt1 = ExampleServer.generateJWT(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId1,
        )

        // Open the first socket connection
        val client1 = CourierClient(jwt = jwt1, userId = userId1).apply {

            val socket = inbox.socket

            socket.receivedMessage = { message ->
                println(message)
                hold1 = false
            }

            socket.connect()
            socket.sendSubscribe()

        }

        val jwt2 = ExampleServer.generateJWT(
            authKey = Env.COURIER_AUTH_KEY,
            userId = userId2,
        )

        // Open the second socket connection
        val client2 = CourierClient(jwt = jwt2, userId = userId2).apply {

            val socket = inbox.socket

            socket.receivedMessage = { message ->
                println(message)
                hold2 = false
            }

            socket.connect()
            socket.sendSubscribe()

        }

        sendMessage(userId = userId1)
        sendMessage(userId = userId2)

        while (hold1 || hold2) {
            // Wait for the message to be received in the sockets
        }

        client1.inbox.socket.disconnect()
        client2.inbox.socket.disconnect()

    }

}