package com.courier.android.models

class CourierException(message: String): Exception(message) {

    companion object {
        val userAlreadySignedIn = CourierException("User already signed in. Please call Courier.shared.signOut() before signing your user in.")
        val missingAccessToken = CourierException("Missing access token")
        val firebaseNotInitialized = CourierException("Firebase not initialized")
        val missingUserId = CourierException("Missing user id")
        val initializationError = CourierException("Courier SDK not initialized. Run Courier.initialize(context) to fix this")
        val jsonParsingError = CourierException("Error Parsing JSON from Courier Server")
        val inboxUserNotFound = CourierException("No user found")
        val inboxWebSocketFail = CourierException("An error occurred. Please try again.")
        val inboxNotInitialized = CourierException("The Courier Inbox is not setup. Please add a CourierInbox view or call Courier.shared.addInboxListener")
        val inboxWebSocketDisconnect = CourierException("An error occurred. Please try again.")
        val inboxMessageNotFound = CourierException("Courier Inbox message not found")
    }

}