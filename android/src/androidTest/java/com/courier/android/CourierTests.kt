package com.courier.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.models.*
import com.courier.android.modules.*
import com.courier.android.repositories.InboxRepository
import com.courier.android.repositories.UsersRepository
import com.courier.android.utils.getLastDeliveredMessage
import com.courier.android.utils.trackNotification
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CourierTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {

        Courier.initialize(context)

    }

    @Test
    fun test_A() = runBlocking {

        Courier.shared.signOut()

    }

    @Test
    fun test_B() = runBlocking {

        print("🔬 Setting FCM Token before User")

        val token = "8af92b6e587cbadf3c2e3d1a"

        Courier.shared.setFCMToken(token = token)

        val fcmToken0 = Courier.shared.fcmToken
        val fcmToken1 = Courier.shared.getToken(provider = "firebase-fcm")
        val fcmToken2 = Courier.shared.getToken(provider = CourierPushProvider.FIREBASE_FCM)

        assertEquals(fcmToken0, token)
        assertEquals(fcmToken1, token)
        assertEquals(fcmToken2, token)

        assertEquals(Courier.shared.userId, null)
        assertEquals(Courier.shared.accessToken, null)

    }

    @Test
    fun test_C() = runBlocking {

        print("🔬 Setting credentials")

        val options = FirebaseOptions.Builder().apply {
            setApiKey(Env.FIREBASE_API_KEY)
            setApplicationId(Env.FIREBASE_APP_ID)
            setProjectId(Env.FIREBASE_PROJECT_ID)
            setGcmSenderId(Env.FIREBASE_GCM_SENDER_ID)
        }.build()

        FirebaseApp.initializeApp(context, options)

        // Firebase is started
        val app = FirebaseApp.getInstance()
        assertEquals(app.options.apiKey, Env.FIREBASE_API_KEY)

        Courier.shared.signIn(
            accessToken = Env.COURIER_AUTH_KEY,
            userId = Env.COURIER_USER_ID,
            clientKey = Env.COURIER_CLIENT_KEY
        )

        assertEquals(Courier.shared.userId, Env.COURIER_USER_ID)
        assertEquals(Courier.shared.clientKey, Env.COURIER_CLIENT_KEY)
        assertEquals(Courier.shared.accessToken, Env.COURIER_AUTH_KEY)

        val fcmToken0 = Courier.shared.fcmToken
        val fcmToken1 = Courier.shared.getToken(provider = "firebase-fcm")
        val fcmToken2 = Courier.shared.getToken(provider = CourierPushProvider.FIREBASE_FCM)

        assertNotNull(fcmToken0)
        assertNotNull(fcmToken1)
        assertNotNull(fcmToken2)

    }

    @Test
    fun test_D() = runBlocking {

        print("🔬 Setting credentials with JWT")

        val jwt = ExampleServer().generateJWT(
            authKey = Env.COURIER_AUTH_KEY,
            userId = Env.COURIER_USER_ID
        )

        Courier.shared.signIn(
            accessToken = jwt,
            userId = Env.COURIER_USER_ID,
            clientKey = Env.COURIER_CLIENT_KEY
        )

        assertEquals(Courier.shared.userId, Env.COURIER_USER_ID)
        assertEquals(Courier.shared.clientKey, Env.COURIER_CLIENT_KEY)
        assertEquals(Courier.shared.accessToken, jwt)

    }

    @Test
    fun test_E() = runBlocking {

        print("🔬 Setting Messaging Tokens")

        val fcm = "6b7a8c9d1e2f3a4g5h6i7j8k"
        val expo = "a1b2c3d4e5f6g7h8i9j0k1l"

        Courier.shared.setFCMToken(token = fcm)
        Courier.shared.setToken(provider = CourierPushProvider.EXPO, token = expo)

        assertEquals(Courier.shared.fcmToken, fcm)
        assertEquals(Courier.shared.getToken(provider = CourierPushProvider.EXPO), expo)

    }

    @Test
    fun test_F() = runBlocking {

        print("🔬 Sending Push")

        val requestId = Courier.shared.sendMessage(
            authKey = Env.COURIER_AUTH_KEY,
            userIds = listOf(Env.COURIER_USER_ID),
            title = "🐤 Chirp Chirp!",
            body = "Message sent from Android Studio tests",
            channels = listOf(
                FirebaseCloudMessagingChannel(
                    data = mapOf(
                        "DATA_FCM" to "YOUR_CUSTOM_VALUE"
                    ),
                    fcmData = mapOf(
                        "FCM_CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                    ),
                    aps = mapOf(
                        "sound" to "ping.aiff",
                        "badge" to 123,
                        "APNS_CUSTOM_NUMBER" to 456,
                        "APNS_CUSTOM_BOOLEAN" to true,
                        "APNS_CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                    )
                )
            ),
        )

        print("Request ID: $requestId")
        assertEquals(requestId.isEmpty(), false)

    }

    @Test
    fun test_G() = runBlocking {

        print("🔬 Tracking Message")

        val message = RemoteMessage.Builder(context.packageName)
            .addData("trackingUrl", "https://af6303be-0e1e-40b5-bb80-e1d9299cccff.ct0.app/t/tzgspbr4jcmcy1qkhw96m0034bvy")
            .build()

        Courier.shared.trackNotification(
            message = message,
            event = CourierPushEvent.DELIVERED
        )

        Courier.shared.trackNotification(
            message = message,
            event = CourierPushEvent.CLICKED
        )

        print("Message tracked")

    }

    private val messageId get() = Courier.shared.inboxMessages?.firstOrNull()?.messageId

    @Test
    fun test_H() = runBlocking {

        print("🔬 Testing Inbox Get Messages")

        var canPage = true
        var error: String? = null

        val listener = Courier.shared.addInboxListener(
            onInitialLoad = {
                print("Loading")
            },
            onError = { e ->
                print(e)
                error = e.message
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->

                if (canPaginate) {

                    Courier.shared.fetchNextPageOfMessages(
                        onSuccess = {
                            print("New messages fetched")
                        },
                        onFailure = { e ->
                            error = e.message
                        }
                    )

                }

                canPage = canPaginate

            }
        )

        while (canPage) {
            // Empty
        }

        print(messageId)

        listener.remove()

        assertEquals(error, null)

    }

    @Test
    fun test_I() = runBlocking {

        print("🔬 Testing Read Message")

        messageId?.let {

            InboxRepository().readMessage(
                clientKey = Env.COURIER_CLIENT_KEY,
                userId = Env.COURIER_USER_ID,
                messageId = it
            )

        }

        assertTrue(true)

    }

    @Test
    fun test_J() = runBlocking {

        print("🔬 Testing Unread Message")

        messageId?.let {

            InboxRepository().unreadMessage(
                clientKey = Env.COURIER_CLIENT_KEY,
                userId = Env.COURIER_USER_ID,
                messageId = it
            )

        }

        assertTrue(true)

    }

    @Test
    fun test_K() = runBlocking {

        print("🔬 Testing Open Message")

        messageId?.let {

            InboxRepository().openMessage(
                clientKey = Env.COURIER_CLIENT_KEY,
                userId = Env.COURIER_USER_ID,
                messageId = it
            )

        }

        assertTrue(true)

    }

    @Test
    fun test_L() = runBlocking {

        print("🔬 Send Inbox Message")

        val requestId = Courier.shared.sendMessage(
            authKey = Env.COURIER_AUTH_KEY,
            userIds = listOf(Env.COURIER_USER_ID),
            title = "🐤 Inbox Message",
            body = "Message sent from Android Studio tests",
            channels = listOf(
                CourierInboxChannel(
                    elements = listOf(
                        CourierElement(
                            type = "action",
                            content = "Button 1",
                            data = mapOf(
                                "CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                            )
                        ),
                        CourierElement(
                            type = "action",
                            content = "Button 2",
                            data = mapOf(
                                "CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                            )
                        )
                    ),
                    data = mapOf(
                        "DATA_INBOX" to "YOUR_CUSTOM_VALUE",
                    )
                )
            )
        )

        print("Request ID: $requestId")
        assertEquals(requestId.isEmpty(), false)

    }

    @Test
    fun test_M() = runBlocking {

        print("🔬 Setting Inbox Pagination Limit")

        Courier.shared.inboxPaginationLimit = 10
        assertEquals(Courier.shared.inboxPaginationLimit, 10)

        Courier.shared.inboxPaginationLimit = -1000
        assertEquals(Courier.shared.inboxPaginationLimit, 1)

        Courier.shared.inboxPaginationLimit = 1000
        assertEquals(Courier.shared.inboxPaginationLimit, 100)

    }

    @Test
    fun test_N() = runBlocking {

        print("🔬 Get All User Preferences")

        val preferences = Courier.shared.getUserPreferences()

        print(preferences)

    }

    @Test
    fun test_O() = runBlocking {

        print("🔬 Get Topic")

        val topic = Courier.shared.getUserPreferenceTopic(
            topicId = "6QHD7Z1D4Q436SMECGXENTQYWVQQ",
        )

        print(topic)

    }

    @Test
    fun test_P() = runBlocking {

        print("🔬 Update Topic")

        Courier.shared.putUserPreferenceTopic(
            topicId = "6QHD7Z1D4Q436SMECGXENTQYWVQQ",
            status = CourierPreferenceStatus.OPTED_IN,
            hasCustomRouting = true,
            customRouting = listOf(CourierPreferenceChannel.SMS, CourierPreferenceChannel.PUSH)
        )

    }

    @Test
    fun test_Z() = runBlocking {

        print("🔬 Signing Out")

        Courier.shared.signOut()

        assertNotNull(Courier.shared.fcmToken)

        assertEquals(Courier.shared.userId, null)
        assertEquals(Courier.shared.accessToken, null)
        assertEquals(Courier.shared.clientKey, null)

    }

}