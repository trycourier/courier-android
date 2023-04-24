package com.courier.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.activity.CourierActivity
import com.courier.android.models.remove
import com.courier.android.modules.*
import com.courier.android.requestNotificationPermission
import com.courier.example.databinding.ActivityMainBinding
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch


class MainActivity : CourierActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Courier.initialize(context = this)

        Courier.shared.addInboxListener(
            onInitialLoad = {
                print("Loading")
            },
            onError = { e ->
                print(e)
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->
                print(messages)
            }
        )

        Courier.shared.addInboxListener(
            onInitialLoad = {
                print("Loading")
            },
            onError = { e ->
                print(e)
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->
                print(messages)
            }
        )

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
            Toast.makeText(
                this@MainActivity,
                "Notification permissions are granted: $hasNotificationPermissions",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
            setup()

        }

    }

    private fun sendPush() {

        lifecycleScope.launch {

            val checkboxes = listOf(
                binding.checkFcm, binding.checkApns
            )

            val providers = checkboxes.filter { it.isChecked }.map { checkbox ->
                return@map when (checkbox.id) {
                    binding.checkFcm.id -> CourierProvider.FCM
                    binding.checkApns.id -> CourierProvider.APNS
                    else -> CourierProvider.FCM
                }
            }

            binding.sendPushButton.isEnabled = false

            try {

                val userId = Env.COURIER_USER_ID

                Courier.shared.sendMessage(
                    authKey = Env.COURIER_AUTH_KEY,
                    userIds = listOf(userId),
                    title = "Hey ${userId}!",
                    body = "This is a test push sent through ${providers.joinToString(" and ") { it.value }}",
                    channels = providers,
                )

            } catch (e: Exception) {

                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()

            }

            binding.sendPushButton.isEnabled = true

        }

    }

    override fun onPushNotificationClicked(message: RemoteMessage) {
        Log.d("Courier", message.toJsonString())
        Toast.makeText(this, "Message clicked:\n${message.data}", Toast.LENGTH_LONG).show()
    }

    override fun onPushNotificationDelivered(message: RemoteMessage) {
        Log.d("Courier", message.toJsonString())
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
                    accessToken = Env.COURIER_ACCESS_TOKEN,
                    userId = Env.COURIER_USER_ID,
                    clientKey = Env.COURIER_CLIENT_KEY
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