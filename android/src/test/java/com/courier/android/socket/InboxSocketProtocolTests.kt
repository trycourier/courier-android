package com.courier.android.socket

import com.courier.android.client.CourierClient
import com.courier.android.models.InboxMessage
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Inbox wire protocol v2 (`iwpv=v2`).
 *
 * These cover the two things that make v2 work and are otherwise invisible when they break: the
 * connection URL must carry `cid` and `iwpv` (the server rejects an IWP connection missing either,
 * and silently downgrades an unknown version to `legacy`), and `clickTrackingId` must read the
 * root-level `trackingIds` that v2 publishes.
 */
class InboxSocketProtocolTests {

    private fun options(
        jwt: String? = "test-jwt",
        clientKey: String? = null,
        connectionId: String? = "conn-123",
        tenantId: String? = null
    ) = CourierClient.Options(
        jwt = jwt,
        clientKey = clientKey,
        userId = "user-1",
        connectionId = connectionId,
        tenantId = tenantId,
        showLogs = false,
        apiUrls = CourierClient.ApiUrls()
    )

    /** Parses the connection URL rather than opening a socket, so these stay pure unit tests. */
    private fun queryParams(options: CourierClient.Options): Map<String, String> {
        val url = InboxSocket.buildUrl(options)
        val query = url.substringAfter("?", "")
        return query.split("&")
            .filter { it.contains("=") }
            .associate { it.substringBefore("=") to it.substringAfter("=") }
    }

    // region Connection URL

    @Test
    fun urlDeclaresProtocolVersionV2() {
        assertEquals("v2", queryParams(options())["iwpv"])
        assertEquals("v2", InboxSocket.PROTOCOL_VERSION)
    }

    @Test
    fun urlCarriesClientIdWhichTheServerRequires() {
        assertEquals("conn-123", queryParams(options(connectionId = "conn-123"))["cid"])
    }

    /**
     * Without a cid the IWP handler closes the connection, so a caller that never set connectionId
     * must still get a usable socket rather than a rejected one.
     */
    @Test
    fun urlGeneratesAClientIdWhenNoneWasProvided() {
        val cid = queryParams(options(connectionId = null))["cid"]
        assertNotNull(cid)
        assertTrue(cid!!.isNotEmpty())
    }

    @Test
    fun urlKeepsJwtAuth() {
        assertEquals("test-jwt", queryParams(options(jwt = "test-jwt"))["auth"])
    }

    /** The client-key path previously returned before any IWP params were appended. */
    @Test
    fun urlCarriesProtocolParamsOnTheClientKeyPath() {
        val params = queryParams(options(jwt = null, clientKey = "ck-1"))
        assertEquals("ck-1", params["clientKey"])
        assertEquals("v2", params["iwpv"])
        assertNotNull(params["cid"])
    }

    // endregion

    // region Tracking ids

    /**
     * v2 publishes `trackingIds` at the root. The nested `data["trackingIds"]` branch that used to
     * be consulted first is gone — it only existed because the `legacy` socket nested them while
     * GraphQL returned them at the root.
     */
    @Test
    fun clickTrackingIdReadsTheRootLevelValue() {
        val json = """
            {
              "messageId": "msg-1",
              "title": "Welcome",
              "trackingIds": { "clickTrackingId": "click-root" },
              "data": { "orderId": "ord_1" }
            }
        """.trimIndent()

        val message = Gson().fromJson(json, InboxMessage::class.java)

        assertEquals("click-root", message.clickTrackingId)
    }

    @Test
    fun clickTrackingIdIsNullWhenAbsent() {
        val message = Gson().fromJson(
            """{ "messageId": "msg-1", "title": "Welcome" }""",
            InboxMessage::class.java
        )

        assertNull(message.clickTrackingId)
    }

    /**
     * A nested-only copy is what a `legacy` frame looks like. v2 never sends it, and reading it
     * would mask a silent downgrade — so this asserts the fallback really is gone.
     */
    @Test
    fun clickTrackingIdIgnoresANestedOnlyCopy() {
        val json = """
            {
              "messageId": "msg-1",
              "data": { "trackingIds": { "clickTrackingId": "click-nested" } }
            }
        """.trimIndent()

        val message = Gson().fromJson(json, InboxMessage::class.java)

        assertNull(message.clickTrackingId)
    }

    // endregion

    // region Frame decoding

    @Test
    fun messageFrameDecodesTheCanonicalMessageFromData() {
        val json = """
            {
              "event": "message",
              "iwpv": "v2",
              "data": {
                "messageId": "msg-1",
                "title": "Welcome",
                "preview": "Preview",
                "created": "2026-08-05T12:00:00.000Z",
                "trackingIds": { "clickTrackingId": "click-1", "readTrackingId": "read-1" },
                "data": { "orderId": "ord_1" }
              }
            }
        """.trimIndent()

        val frame = Gson().fromJson(json, InboxSocket.MessageFrame::class.java)

        assertEquals("message", frame.event)
        assertEquals("v2", frame.iwpv)
        assertEquals("msg-1", frame.data?.messageId)
        assertEquals("Welcome", frame.data?.title)
        assertEquals("click-1", frame.data?.clickTrackingId)
        assertEquals("read-1", frame.data?.trackingIds?.readTrackingId)
        // `data` keeps only non-promoted keys under v2.
        assertEquals("ord_1", frame.data?.data?.get("orderId"))
    }

    /**
     * A heartbeat must be recognized as a control frame. It has no `event`, so if it fell through to
     * the message decode it would surface as a spurious error every ping interval — and an
     * unanswered ping gets the connection terminated by the server.
     */
    @Test
    fun pingDecodesAsAControlFrame() {
        val control = Gson().fromJson(
            """{ "iwpv": "v2", "tid": "tid-1", "action": "ping" }""",
            InboxSocket.ControlFrame::class.java
        )

        assertEquals("ping", control.action)
        assertEquals("tid-1", control.tid)
        assertNull(control.response)
    }

    @Test
    fun ackDecodesAsAControlFrame() {
        val control = Gson().fromJson(
            """{ "tid": "tid-1", "response": "ack" }""",
            InboxSocket.ControlFrame::class.java
        )

        assertEquals("ack", control.response)
        assertNull(control.action)
    }

    // endregion
}
