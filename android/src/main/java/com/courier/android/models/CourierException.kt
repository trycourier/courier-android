package com.courier.android.models

class CourierException(message: String): Exception(message) {

    companion object {
        val missingAccessToken = CourierException("Missing access token")
        val firebaseNotInitialized = CourierException("Firebase not initialized")
        val missingUserId = CourierException("Missing user id")
        val initializationError = CourierException("Courier SDK not initialized. Run Courier.initialize(context) to fix this")
        val jsonParsingError = CourierException("Error Parsing JSON from Courier Server")
    }

}