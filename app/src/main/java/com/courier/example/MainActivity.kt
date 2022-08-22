package com.courier.example

import android.os.Bundle
import android.widget.Toast
import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.notifications.CourierActivity
import com.courier.android.requestNotificationPermission
import com.courier.android.sendPush
import com.courier.example.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.RemoteMessage

class MainActivity : CourierActivity() {

    companion object {
        const val COURIER_USER_ID = "example_user"
        const val COURIER_ACCESS_TOKEN = "pk_prod_G7KDRN4NAXMR9BMBQVDN2375ZHE7"
        const val FIREBASE_API_KEY = "AIzaSyDide6nNMYZQ312MIYT4z6FOKqz3RH-F2I"
        const val FIREBASE_APP_ID = "1:939573391521:android:935a7be3ae1099200d80e0"
        const val FIREBASE_PROJECT_ID = "courier-example"
        const val FIREBASE_GCM_SENDER_ID = "939573391521"
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

        Courier.instance.setCredentials(
            accessToken = COURIER_ACCESS_TOKEN,
            userId = COURIER_USER_ID,
            onSuccess = {
                print("Credentials set")
            },
            onFailure = { e ->
                print(e)
            }
        )

    }

    private fun sendPush() {

        requestNotificationPermission { granted ->

            if (granted) {

                Courier.sendPush(
                    authKey = COURIER_ACCESS_TOKEN,
                    userId = COURIER_USER_ID,
                    title = "This is a title",
                    body = "This is a message",
                    providers = listOf(CourierProvider.FCM),
                    onSuccess = { requestId ->
                        print(requestId)
                    },
                    onFailure = { e ->
                        print(e)
                    }
                )

            }

        }

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