package com.courier.android.client

import com.courier.android.ClientBuilder
import com.courier.android.Env
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.models.CourierPreferenceStatus
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PreferenceClientTests {

    @Test
    fun getPreferences() = runBlocking {

        val client = ClientBuilder.build()

        val preferences = client.preferences.getUserPreferences(
            paginationCursor = null,
        )

        assertTrue(preferences.items.isNotEmpty())

    }

    @Test
    fun getTopic() = runBlocking {

        val client = ClientBuilder.build()

        val topic = client.preferences.getUserPreferenceTopic(
            topicId = Env.COURIER_PREFERENCE_TOPIC_ID,
        )

        assertTrue(topic.topicId == Env.COURIER_PREFERENCE_TOPIC_ID)

    }

    @Test
    fun updateTopic() = runBlocking {

        val client = ClientBuilder.build()

        client.preferences.putUserPreferenceTopic(
            topicId = Env.COURIER_PREFERENCE_TOPIC_ID,
            status = CourierPreferenceStatus.OPTED_IN,
            hasCustomRouting = true,
            customRouting = listOf(CourierPreferenceChannel.PUSH),
        )

    }

}