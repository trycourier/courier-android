package com.courier.android.client

import org.junit.Assert.assertEquals
import org.junit.Test

class CourierClientApiUrlsTests {

    @Test
    fun defaultApiUrlsUseCurrentInboxHosts() {
        val urls = CourierClient.ApiUrls()

        assertEquals("https://api.courier.com", urls.rest)
        assertEquals("https://api.courier.com/client/q", urls.graphql)
        assertEquals("https://inbox.courier.io/q", urls.inboxGraphql)
        assertEquals("wss://realtime.courier.io", urls.inboxWebSocket)
    }

    @Test
    fun euApiUrlsPreset() {
        val urls = CourierClient.ApiUrls.eu()

        assertEquals("https://api.eu.courier.com", urls.rest)
        assertEquals("https://api.eu.courier.com/client/q", urls.graphql)
        assertEquals("https://inbox.eu.courier.io/q", urls.inboxGraphql)
        assertEquals("wss://realtime.eu.courier.io", urls.inboxWebSocket)
    }
}
