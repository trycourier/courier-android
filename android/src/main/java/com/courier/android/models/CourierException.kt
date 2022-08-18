package com.courier.android.models

class CourierException(message: String): Exception(message) {

    companion object {
        val missingAccessToken = CourierException("Missing access token")
        val firebaseNotInitialized = CourierException("Firebase not initialized")
        val missingUserId = CourierException("Missing user id")
        val requestError = CourierException("Error performing request")
    }

}