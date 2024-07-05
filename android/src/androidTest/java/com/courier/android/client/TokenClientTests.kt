package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenClientTests {

    private val exampleToken = "f371039a5459ee369f7223cf94cc8638"

    @Test
    fun upsertToken() = runBlocking {

        val client = ClientBuilder.build()

        client.tokens.putUserToken(
            token = exampleToken,
            provider = "firebase-fcm",
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