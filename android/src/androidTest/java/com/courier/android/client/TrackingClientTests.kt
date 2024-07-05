package com.courier.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.courier.android.models.CourierTrackingEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackingClientTests {

    private val trackingUrl = "https://af6303be-0e1e-40b5-bb80-e1d9299cccff.ct0.app/t/tzgspbr4jcmcy1qkhw96m0034bvy"

    @Test
    fun trackDelivered() = runBlocking {

        val client = ClientBuilder.build()

        client.tracking.postTrackingUrl(
            url = trackingUrl,
            event = CourierTrackingEvent.DELIVERED,
        )

    }

    @Test
    fun trackClicked() = runBlocking {

        val client = ClientBuilder.build()

        client.tracking.postTrackingUrl(
            url = trackingUrl,
            event = CourierTrackingEvent.CLICKED,
        )

    }

}