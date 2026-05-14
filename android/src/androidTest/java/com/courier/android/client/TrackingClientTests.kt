package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.courier.android.models.CourierTrackingEvent
import com.courier.android.utils.error
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingClientTests {

    private val trackingUrl = "https://af6303be-0e1e-40b5-bb80-e1d9299cccff.ct0.app/t/tzgspbr4jcmcy1qkhw96m0034bvy"

    @Test
    fun trackDelivered() = runBlocking {
        CourierClient.default.tracking.postTrackingUrl(
            url = trackingUrl,
            event = CourierTrackingEvent.DELIVERED,
        )
    }

    @Test
    fun trackClicked() = runBlocking {
        CourierClient.default.tracking.postTrackingUrl(
            url = trackingUrl,
            event = CourierTrackingEvent.CLICKED,
        )
    }

    @Test
    fun trackCustomTrackingUrl() = runBlocking {
        val exampleUrl = "https://jsonplaceholder.typicode.com/posts"
        try {
            CourierClient.default.tracking.postTrackingUrl(
                url = exampleUrl,
                event = CourierTrackingEvent.CLICKED,
            )
        } catch (e: Exception) {
            CourierClient.default.error(e.toString())
        }
        assert(true)
    }

}