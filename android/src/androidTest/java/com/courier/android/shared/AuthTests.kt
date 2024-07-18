package com.courier.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.Env
import com.courier.android.models.remove
import com.courier.android.modules.accessToken
import com.courier.android.modules.addAuthenticationListener
import com.courier.android.modules.clientKey
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.android.modules.userId
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    @Test
    fun signUserIn() = runBlocking {

        var hold = true

        val listener = Courier.shared.addAuthenticationListener { userId ->
            print(userId)
            if (userId != null) {
                hold = false
            }
        }

        Courier.shared.signIn(
            accessToken = Env.COURIER_AUTH_KEY,
            userId = Env.COURIER_USER_ID,
        )

        assertEquals(Courier.shared.accessToken, Env.COURIER_AUTH_KEY)
        assertEquals(Courier.shared.userId, Env.COURIER_USER_ID)
        assertEquals(Courier.shared.clientKey, null)

        while (hold) {
            // Hold for auth listener
        }

        listener.remove()

    }

    @Test
    fun signUserOut() = runBlocking {

        var hold = true

        val listener = Courier.shared.addAuthenticationListener { userId ->
            print(userId)
            if (userId == null) {
                hold = false
            }
        }

        Courier.shared.signOut()

        assertEquals(Courier.shared.accessToken, null)
        assertEquals(Courier.shared.userId, null)
        assertEquals(Courier.shared.clientKey, null)

        while (hold) {
            // Hold for auth listener
        }

        listener.remove()

    }

}