package com.courier.android.singleton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import junit.framework.TestCase.assertEquals
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

    }

}