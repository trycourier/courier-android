package com.courier.example

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.activity.CourierActivity
import com.courier.android.notifications.CourierPushNotificationCallbacks
import com.courier.android.requestNotificationPermission
import com.courier.android.sendPush
import com.courier.example.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch


class MainActivity : CourierActivity(), CourierPushNotificationCallbacks {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Courier.initialize(this)
        pushNotificationCallbacks = this

        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        binding.authButton.isEnabled = false
        binding.sendPushButton.isEnabled = false

        lifecycleScope.launch {
            setup()
            refresh()
        }

        binding.sendPushButton.setOnClickListener {
            sendPush()
        }

    }

    private suspend fun setup() {

        try {

            val hasNotificationPermissions = requestNotificationPermission()
            Toast.makeText(this@MainActivity, "Notification permissions are granted: $hasNotificationPermissions", Toast.LENGTH_LONG).show()


            // Start firebase
            // This must be set before you can sync FCM tokens in Courier
            val options = FirebaseOptions.Builder().apply {
                setApiKey(Env.FIREBASE_API_KEY)
                setApplicationId(Env.FIREBASE_APP_ID)
                setProjectId(Env.FIREBASE_PROJECT_ID)
                setGcmSenderId(Env.FIREBASE_GCM_SENDER_ID)
            }.build()

            FirebaseApp.initializeApp(this@MainActivity, options)

            // Init Courier
            Courier.initialize(context = this)

            Toast.makeText(this, "SDK Initialized", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {

            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
            setup()

        }

    }

    private fun sendPush() {

        lifecycleScope.launch {

            binding.sendPushButton.isEnabled = false

            try {

                Courier.shared.sendPush(
                    authKey = Env.COURIER_AUTH_KEY,
                    userId = Env.COURIER_USER_ID,
                    title = "Hey ${Courier.shared.userId}!",
                    body = "This is a test message",
                    isProduction = false,
                    providers = listOf(CourierProvider.FCM),
                )

            } catch (e: Exception) {

                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()

            }

            binding.sendPushButton.isEnabled = true

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

    private fun refresh() {

        binding.sendPushButton.isEnabled = true
        binding.authButton.isEnabled = true

        if (Courier.shared.userId == null) {
            binding.authTextView.text = "Courier user not signed in"
            binding.authButton.text = "Sign In"
            binding.authButton.setOnClickListener {
                signIn()
            }
        } else {
            binding.authTextView.text = "Courier user signed in: ${Courier.shared.userId}"
            binding.authButton.text = "Sign Out"
            binding.authButton.setOnClickListener {
                signOut()
            }
        }

    }

    private fun signIn() {
        lifecycleScope.launch {

            binding.authButton.isEnabled = false

            try {
                Courier.shared.signIn(
                    accessToken = Env.COURIER_AUTH_KEY,
                    userId = Env.COURIER_USER_ID
                )

                refresh()

                Toast.makeText(this@MainActivity, "Courier user signed in", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {

                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()

            }

            binding.authButton.isEnabled = true

        }
    }

    private fun signOut() {
        lifecycleScope.launch {

            binding.authButton.isEnabled = false

            try {

                Courier.shared.signOut()

                refresh()

                Toast.makeText(this@MainActivity, "Courier user signed out", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {

                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()

            }

            binding.authButton.isEnabled = true

        }
    }

}