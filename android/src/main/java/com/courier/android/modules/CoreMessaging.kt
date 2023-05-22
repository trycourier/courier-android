package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.models.CourierChannel
import com.courier.android.models.CourierProvider
import com.courier.android.repositories.MessagingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class CoreMessaging {

    private val messagingRepo = MessagingRepository()

    internal suspend fun sendMessage(authKey: String, userIds: List<String>, title: String, body: String, channels: List<CourierChannel>): String {
        return messagingRepo.send(
            authKey = authKey,
            userIds = userIds,
            title = title,
            body = body,
            channels = channels
        )
    }

}

/**
 * Extensions
 */

/**
 * Sends a message via the Courier /send api to the user id you provide
 * More info: https://www.courier.com/docs/reference/send/message/
 */
suspend fun Courier.sendMessage(authKey: String, userIds: List<String>, title: String, body: String, channels: List<CourierChannel>): String {
    return messaging.sendMessage(
        authKey = authKey,
        userIds = userIds,
        title = title,
        body = body,
        channels = channels
    )
}

fun Courier.sendMessage(authKey: String, userIds: List<String>, title: String, body: String, channels: List<CourierChannel>, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) = Courier.coroutineScope.launch(Dispatchers.IO) {
    try {
        val requestId = sendMessage(
            authKey = authKey,
            userIds = userIds,
            title = title,
            body = body,
            channels = channels
        )
        onSuccess(requestId)
    } catch (e: Exception) {
        onFailure(e)
    }
}