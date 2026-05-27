package com.courier.android.unit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.courier.android.Courier
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Proves that [Courier.onMessageReceived] blocks the calling thread until the
 * DELIVERED tracking POST completes, which is critical for killed-state FCM
 * delivery where Android tears the process down as soon as
 * `FirebaseMessagingService.onMessageReceived` returns.
 *
 * Uses [MockWebServer] to control network latency and a single-thread executor
 * to simulate the worker thread FCM dispatches `onMessageReceived` on.
 */
@RunWith(AndroidJUnit4::class)
class PushDeliveryThreadingTests {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        Courier.initialize(context)
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun onMessageReceived_blocksUntilTrackingPostCompletes() {
        server.enqueue(
            MockResponse()
                .setHeadersDelay(800, TimeUnit.MILLISECONDS)
                .setResponseCode(200)
        )
        server.start()

        val data = mapOf("trackingUrl" to server.url("/t/abc").toString())

        val executor = Executors.newSingleThreadExecutor { Thread(it, "fcm-worker-sim") }
        val startNs = System.nanoTime()
        executor.submit { Courier.onMessageReceived(data) }.get(10, TimeUnit.SECONDS)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

        val request = server.takeRequest(100, TimeUnit.MILLISECONDS)
        assertNotNull("tracking POST should have been observed by MockWebServer", request)
        assertEquals("POST", request!!.method)
        assertTrue(
            "onMessageReceived must block until the POST completes; elapsed=${elapsedMs}ms",
            elapsedMs >= 700
        )
    }

    @Test(timeout = 15_000)
    fun onMessageReceived_respectsTimeoutWhenServerHangs() {
        server.enqueue(
            MockResponse()
                .setHeadersDelay(30, TimeUnit.SECONDS)
                .setResponseCode(200)
        )
        server.start()

        val data = mapOf("trackingUrl" to server.url("/t/slow").toString())

        val startNs = System.nanoTime()
        Courier.onMessageReceived(data)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

        assertTrue(
            "must not block beyond MESSAGE_RECEIVED_TIMEOUT_MS (~8s); elapsed=${elapsedMs}ms",
            elapsedMs in 6_000..12_000
        )
    }

    @Test
    fun onMessageReceived_returnsImmediatelyWhenNoTrackingUrl() {
        server.start()

        val startNs = System.nanoTime()
        Courier.onMessageReceived(mapOf("title" to "t", "body" to "b"))
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L

        assertTrue(
            "no trackingUrl should not block; elapsed=${elapsedMs}ms",
            elapsedMs < 2_000
        )
    }
}
