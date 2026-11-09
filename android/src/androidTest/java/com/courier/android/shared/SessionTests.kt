package com.courier.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.ExampleServer
import com.courier.android.models.CourierException
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.removeInboxListener
import com.courier.android.modules.restoreClient
import com.courier.android.modules.setToken
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.app.Env
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

// The session has to survive a relaunch, and signing out has to take the device's tokens with it
@RunWith(AndroidJUnit4::class)
class SessionTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    private suspend fun signInNewUser(): String {
        val userId = "android-session-${UUID.randomUUID()}"
        val jwt = ExampleServer.generateJWT(authKey = Env.COURIER_AUTH_KEY, userId = userId)
        Courier.shared.signIn(userId = userId, accessToken = jwt, showLogs = true)
        return userId
    }

    private suspend fun waitForToken(userId: String, token: String, present: Boolean) {
        repeat(20) {
            if (ExampleServer.hasUserToken(Env.COURIER_AUTH_KEY, userId, token) == present) return
            delay(1000)
        }
        assertEquals("token $token present for $userId", present, ExampleServer.hasUserToken(Env.COURIER_AUTH_KEY, userId, token))
    }

    @Test
    fun relaunchRestoresTheSignedInClient() = runBlocking {

        val userId = signInNewUser()
        val signedIn = Courier.shared.client!!

        // What a relaunched app builds from the saved session
        val restored = Courier.shared.restoreClient()

        assertNotNull(restored)
        assertEquals(userId, restored!!.options.userId)
        assertEquals(signedIn.options.apiUrls, restored.options.apiUrls)
        assertEquals(signedIn.options.showLogs, restored.options.showLogs)

        // And it can make requests as that user
        assertTrue(restored.inbox.getUnreadMessageCount() >= 0)

        Courier.shared.signOut()

    }

    @Test
    fun signOutLeavesNothingToRestore() = runBlocking {

        signInNewUser()
        Courier.shared.signOut()

        assertNull(Courier.shared.client)
        assertNull(Courier.shared.restoreClient())

    }

    @Test
    fun expiredTokenFailsCleanlyAndRecovers() = runBlocking {

        val userId = "android-session-${UUID.randomUUID()}"
        val shortJwt = ExampleServer.generateJWT(authKey = Env.COURIER_AUTH_KEY, userId = userId, expiresIn = "10s")
        Courier.shared.signIn(userId = userId, accessToken = shortJwt, showLogs = true)

        delay(13_000)

        // Requests fail with an error the app can handle, not a crash
        val failure = runCatching { Courier.shared.client!!.inbox.getUnreadMessageCount() }.exceptionOrNull()
        assertTrue("expected a CourierException, got $failure", failure is CourierException)

        // And the inbox reports it through its listener
        var inboxError: String? = null
        val listener = Courier.shared.addInboxListener(onError = { inboxError = it.message ?: "error" })
        repeat(20) { if (inboxError == null) delay(500) }
        Courier.shared.removeInboxListener(listener)
        assertNotNull("the inbox listener should get an error", inboxError)

        // Signing in again with a fresh token recovers
        val freshJwt = ExampleServer.generateJWT(authKey = Env.COURIER_AUTH_KEY, userId = userId)
        Courier.shared.signIn(userId = userId, accessToken = freshJwt, showLogs = true)
        assertTrue(Courier.shared.client!!.inbox.getUnreadMessageCount() >= 0)

        Courier.shared.signOut()

    }

    @Test
    fun signOutDeletesTheDeviceTokens() = runBlocking {

        val userId = signInNewUser()
        val token = "android-session-token-${UUID.randomUUID()}"

        Courier.shared.setToken(provider = "android-session-test", token = token)
        waitForToken(userId, token, present = true)

        Courier.shared.signOut()
        waitForToken(userId, token, present = false)

        assertFalse(ExampleServer.hasUserToken(Env.COURIER_AUTH_KEY, userId, token))

    }

}
