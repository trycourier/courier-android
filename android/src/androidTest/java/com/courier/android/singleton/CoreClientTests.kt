package com.courier.android.singleton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.client.ClientBuilder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreClientTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    @Test
    fun clientSingletonTest() = runBlocking {

        UserBuilder.authenticate()

        val client1 = Courier.shared.client
        val client2 = Courier.shared.client

        assertEquals(client1, client2)

        val client3 = ClientBuilder.build()

        assertNotSame(client1, client3)
        assertNotSame(client2, client3)

    }

}