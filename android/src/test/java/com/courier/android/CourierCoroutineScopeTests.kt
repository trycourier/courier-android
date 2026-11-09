package com.courier.android

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

// Work the SDK runs in the background (socket events, lifecycle hooks, tracking) must not crash
// the app when it fails, and one failure must not stop every later SDK call from running.
class CourierCoroutineScopeTests {

    private fun captureUncaught(block: () -> Unit): List<Throwable> {
        val uncaught = mutableListOf<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> synchronized(uncaught) { uncaught.add(error) } }
        try {
            block()
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
        return synchronized(uncaught) { uncaught.toList() }
    }

    @Test
    fun failedWorkIsNotThrownToTheApp() {
        var ran = false
        val uncaught = captureUncaught {
            runBlocking {
                Courier.coroutineScope.launch(Dispatchers.IO) {
                    ran = true
                    throw UnknownHostException("tracking.example.com")
                }.join()
            }
        }
        assertTrue("The failing work never ran", ran)
        assertEquals(emptyList<Throwable>(), uncaught)
    }

    @Test
    fun laterWorkStillRunsAfterAFailure() = runBlocking {
        captureUncaught {
            runBlocking {
                Courier.coroutineScope.launch(Dispatchers.IO) { error("boom") }.join()
            }
        }

        val ran = CompletableDeferred<Boolean>()
        Courier.coroutineScope.launch(Dispatchers.IO) { ran.complete(true) }
        assertTrue("The SDK scope stopped running work after one failure", withTimeoutOrNull(5_000) { ran.await() } ?: false)
    }

}
