package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.ClientBuilder
import com.courier.android.models.CourierDevice
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenClientTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val exampleToken = "f371039a5459ee369f7223cf94cc8638"

    @Test
    fun upsertToken() = runBlocking {

        val client = ClientBuilder.build()

        client.tokens.putUserToken(
            token = exampleToken,
            provider = "firebase-fcm",
            device = CourierDevice.current(context),
        )

    }

    @Test
    fun upsertTokenWithCustomDevice() = runBlocking {

        val client = ClientBuilder.build()

        val device = CourierDevice(
            appId = "APP_ID",
            adId = "AD_ID",
            deviceId = "DEVICE_ID",
            platform = "android",
            manufacturer = "Google",
            model = "Pixel 99"
        )

        client.tokens.putUserToken(
            token = exampleToken,
            provider = "firebase-fcm",
            device = device
        )

    }

    @Test
    fun removeToken() = runBlocking {

        val client = ClientBuilder.build()

        client.tokens.deleteUserToken(
            token = exampleToken,
        )

    }

}