package com.courier.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.models.CourierException
import com.courier.android.models.CourierProvider
import com.courier.android.models.CourierPushEvent
import com.courier.android.modules.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class CourierTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun test1() = runBlocking {

        print("🔬 Start Courier SDK")

        Courier.initialize(context)

    }

    @Test
    fun test2() = runBlocking {

        print("🔬 Setting FCM Token before User")

        Courier.shared.setFCMToken(token = "something_that_will_fail")

        val fcmToken = Courier.shared.getFCMToken()

        assertEquals(fcmToken, null)
        assertEquals(Courier.shared.userId, null)
        assertEquals(Courier.shared.accessToken, null)

    }

    @Test
    fun test3() = runBlocking {

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
            userId = Env.COURIER_USER_ID
        )

        assertEquals(Courier.shared.userId, Env.COURIER_USER_ID)
        assertEquals(Courier.shared.accessToken, Env.COURIER_AUTH_KEY)

        val fcmToken = Courier.shared.getFCMToken()
        assertNotNull(fcmToken)

    }

    @Test
    fun test4() = runBlocking {

        print("🔬 Setting FCM Token")

        Courier.shared.setFCMToken(
            token = "something_that_will_succeed"
        )

        val fcmToken = Courier.shared.getFCMToken()
        assertNotNull(fcmToken)

    }

    @Test
    fun test5() = runBlocking {

        print("🔬 Sending Push")

        val requestId = Courier.shared.sendMessage(
            authKey = Env.COURIER_AUTH_KEY,
            userIds = listOf(Env.COURIER_USER_ID),
            title = "🐤 Chirp Chirp!",
            body = "Message sent from Android Studio tests",
            channels = listOf(CourierProvider.FCM),
        )

        print("Request ID: $requestId")
        assertEquals(requestId.isEmpty(), false)

    }

    @Test
    fun test6() = runBlocking {

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

    @Test
    fun test7() = runBlocking {

        print("🔬 Signing Out")

        Courier.shared.signOut()

        val fcmToken = Courier.shared.getFCMToken()
        assertNotNull(fcmToken)

        assertEquals(Courier.shared.userId, null)
        assertEquals(Courier.shared.accessToken, null)

    }

}