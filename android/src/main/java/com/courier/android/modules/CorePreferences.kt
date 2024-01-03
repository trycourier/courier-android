package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.managers.UserManager
import com.courier.android.models.*
import com.courier.android.repositories.MessagingRepository
import com.courier.android.repositories.UsersRepository
import kotlinx.coroutines.*

internal class CorePreferences {

    private val usersRepo = UsersRepository()

    internal suspend fun getUserPreferences(paginationCursor: String? = null): CourierUserPreferences {

        if (Courier.shared.accessToken == null) {
            throw CourierException.missingAccessToken
        }

        if (Courier.shared.userId == null) {
            throw CourierException.missingUserId
        }

        return usersRepo.getUserPreferences(
            accessToken = Courier.shared.accessToken!!,
            userId = Courier.shared.userId!!,
            paginationCursor = paginationCursor
        )

    }

    internal suspend fun putUserPreferenceTopic(topicId: String, status: CourierPreferenceStatus, hasCustomRouting: Boolean, customRouting: List<CourierPreferenceChannel>) {

        if (Courier.shared.accessToken == null) {
            throw CourierException.missingAccessToken
        }

        if (Courier.shared.userId == null) {
            throw CourierException.missingUserId
        }

        return usersRepo.putUserPreferenceTopic(
            accessToken = Courier.shared.accessToken!!,
            userId = Courier.shared.userId!!,
            topicId = topicId,
            status = status,
            hasCustomRouting = hasCustomRouting,
            customRouting = customRouting
        )

    }

    internal suspend fun getUserPreferenceTopic(topicId: String): CourierPreferenceTopic {

        if (Courier.shared.accessToken == null) {
            throw CourierException.missingAccessToken
        }

        if (Courier.shared.userId == null) {
            throw CourierException.missingUserId
        }

        return usersRepo.getUserPreferenceTopic(
            accessToken = Courier.shared.accessToken!!,
            userId = Courier.shared.userId!!,
            topicId = topicId
        )

    }

}

/**
 * Extensions
 */

suspend fun Courier.getUserPreferences(paginationCursor: String? = null): CourierUserPreferences {
    return preferences.getUserPreferences(paginationCursor)
}

fun Courier.getUserPreferences(paginationCursor: String? = null, onSuccess: (CourierUserPreferences) -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    try {
        val preferences = getUserPreferences(paginationCursor)
        onSuccess(preferences)
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun Courier.putUserPreferenceTopic(topicId: String, status: CourierPreferenceStatus, hasCustomRouting: Boolean, customRouting: List<CourierPreferenceChannel>) {
    return preferences.putUserPreferenceTopic(topicId, status, hasCustomRouting, customRouting)
}

fun Courier.putUserPreferenceTopic(topicId: String, status: CourierPreferenceStatus, hasCustomRouting: Boolean, customRouting: List<CourierPreferenceChannel>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    try {
        putUserPreferenceTopic(topicId, status, hasCustomRouting, customRouting)
        onSuccess()
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun Courier.getUserPreferenceTopic(topicId: String): CourierPreferenceTopic {
    return preferences.getUserPreferenceTopic(topicId)
}

fun Courier.getUserPreferenceTopic(topicId: String, onSuccess: (CourierPreferenceTopic) -> Unit, onFailure: (Exception) -> Unit) = coroutineScope.launch(Dispatchers.Main) {
    try {
        val topic = getUserPreferenceTopic(topicId)
        onSuccess(topic)
    } catch (e: Exception) {
        onFailure(e)
    }
}