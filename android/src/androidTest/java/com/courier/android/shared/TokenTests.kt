package com.courier.android.shared

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import com.courier.android.UserBuilder
import com.courier.android.models.CourierDevice
import com.courier.android.models.CourierPushProvider
import com.courier.android.modules.getToken
import com.courier.android.modules.setFcmToken
import com.courier.android.modules.setToken
import com.courier.android.modules.signOut
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenTests {

    private val exampleToken = "f371039a5459ee369f7223cf94cc8638"
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Courier.initialize(context)
    }

    @Test
    fun defaultDeviceToken() {
        val device = CourierDevice()
        assertEquals(device.appId, context.packageName)
    }

    @Test
    fun customDeviceToken() {
        val device = CourierDevice(appId = "Example")
        assertEquals(device.appId, "Example")
    }

    @Test
    fun addToken() = runBlocking {

        UserBuilder.authenticate()

        val provider = CourierPushProvider.FIREBASE_FCM

        Courier.shared.setToken(
            provider = provider,
            token = exampleToken,
        )

        val userToken = Courier.shared.getToken(
            provider = provider
        )

        assertEquals(userToken, exampleToken)

    }

    @Test
    fun tokensRemoved() = runBlocking {

        UserBuilder.authenticate()

        Courier.shared.setFcmToken(
            token = exampleToken
        )

        val provider = CourierPushProvider.EXPO

        Courier.shared.setToken(
            provider = provider,
            token = exampleToken,
        )

        Courier.shared.signOut()

        val userToken = Courier.shared.getToken(
            provider = provider,
        )

        assertEquals(Courier.shared.fcmToken, exampleToken)
        assertEquals(userToken, exampleToken)

    }

}