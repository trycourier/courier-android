package com.courier.example

import android.content.SharedPreferences
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
import kotlinx.coroutines.launch


class MainActivity : CourierActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {

            try {
                setup()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
                setup()
            }

        }

    }

    private suspend fun setup() {

        val prefs = showSDKConfig(
            activity = this@MainActivity,
            items = listOf(
                DialogItem("FIREBASE_API_KEY", "Firebase API Key"),
                DialogItem("FIREBASE_APP_ID", "Firebase App ID"),
                DialogItem("FIREBASE_PROJECT_ID", "Firebase Project ID"),
                DialogItem("FIREBASE_GCM_SENDER_ID", "Firebase GCM Sender ID"),
                DialogItem("COURIER_ACCESS_TOKEN", "Courier Access Token"),
                DialogItem("COURIER_AUTH_KEY", "Courier Auth Key"),
                DialogItem("COURIER_USER_ID", "Courier User ID"),
            )
        )

        // Start firebase
        // This must be set before you can sync FCM tokens in Courier
        val options = FirebaseOptions.Builder().apply {
            setApiKey(prefs.getString("FIREBASE_API_KEY", "") ?: "")
            setApplicationId(prefs.getString("FIREBASE_APP_ID", "") ?: "")
            setProjectId(prefs.getString("FIREBASE_PROJECT_ID", "") ?: "")
            setGcmSenderId(prefs.getString("FIREBASE_GCM_SENDER_ID", "") ?: "")
        }.build()

        FirebaseApp.initializeApp(this@MainActivity, options)

        // Set layout
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
            sendPushButton.setOnClickListener {
                sendPush(prefs)
            }
        }

        Courier.instance.setUser(
            accessToken = prefs.getString("COURIER_ACCESS_TOKEN", "") ?: "",
            userId = prefs.getString("COURIER_USER_ID", "") ?: ""
        )

    }

    private fun sendPush(prefs: SharedPreferences) {

        lifecycleScope.launch {

            try {

                val granted = requestNotificationPermission()

                if (granted) {

                    Courier.instance.sendPush(
                        authKey = prefs.getString("COURIER_AUTH_KEY", "") ?: "",
                        userId = prefs.getString("COURIER_USER_ID", "") ?: "",
                        title = "This is a title",
                        body = "This is a message",
                        providers = listOf(CourierProvider.FCM),
                    )

                }

            } catch (e: Exception) {

                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()

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