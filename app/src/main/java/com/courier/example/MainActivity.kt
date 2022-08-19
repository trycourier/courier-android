package com.courier.example

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.notifications.CourierActivity
import com.courier.android.requestNotificationPermission
import com.courier.android.sendPush
import com.courier.example.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : CourierActivity() {

    companion object {
        const val COURIER_USER_ID = "example_user_2"
        const val COURIER_ACCESS_TOKEN = "pk_prod_X9SHD669JF400NHY56KYPTE639HH"
        const val FIREBASE_API_KEY = "AIzaSyC_9Bq05Ywuy3mjAOkF8rB0LkmyYUoQIrA"
        const val FIREBASE_APP_ID = "1:694725526129:android:77c71528200b105c8811d0"
        const val FIREBASE_PROJECT_ID = "test-fcm-e7ddc"
        const val FIREBASE_GCM_SENDER_ID = "694725526129"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start firebase
        // This must be set before you can sync FCM tokens in Courier
        val options = FirebaseOptions.Builder().apply {
            setApiKey(FIREBASE_API_KEY)
            setApplicationId(FIREBASE_APP_ID)
            setProjectId(FIREBASE_PROJECT_ID)
            setGcmSenderId(FIREBASE_GCM_SENDER_ID)
        }.build()

        FirebaseApp.initializeApp(this, options)

        // Set layout
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
            sendPushButton.setOnClickListener {
                sendPush()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                // Sign into Courier
                Courier.instance.setCredentials(
                    accessToken = COURIER_ACCESS_TOKEN,
                    userId = COURIER_USER_ID
                )

//                Courier.instance.signOut()

            } catch (e: Exception) {
                print(e)
            }

        }

    }

    private fun sendPush() = lifecycleScope.launch(Dispatchers.Main) {

        val granted = requestNotificationPermission()

        if (granted) {

            try {

                val requestId = Courier.sendPush(
                    authKey = COURIER_ACCESS_TOKEN,
                    userId = COURIER_USER_ID,
                    title = "Test Push Notification",
                    body = "Hello from Courier! 🐣",
                    providers = listOf(CourierProvider.FCM)
                )
                print(requestId)

            } catch (e: Exception) {
                print(e)
            }

        }

//        requestNotificationPermission { granted ->
//
//            if (granted) {
//
//                Courier.sendPush(
//                    authKey = COURIER_ACCESS_TOKEN,
//                    userId = COURIER_USER_ID,
//                    title = "This is a title",
//                    body = "This is a message",
//                    providers = listOf(CourierProvider.FCM),
//                    onSuccess = { requestId ->
//                        print(requestId)
//                    },
//                    onFailure = { e ->
//                        print(e)
//                    }
//                )
//
//            }
//
//        }

    }

    override fun onPushNotificationClicked(message: RemoteMessage) {
        print(message)
        Toast.makeText(this, "Message clicked:\n${message.data}", Toast.LENGTH_LONG).show()
    }

    override fun onPushNotificationDelivered(message: RemoteMessage) {
        print(message)
        Toast.makeText(this, "Message delivered:\n${message.data}", Toast.LENGTH_LONG).show()
    }

}