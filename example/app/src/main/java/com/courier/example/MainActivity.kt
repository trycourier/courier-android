package com.courier.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.notifications.CourierActivity
import com.courier.android.sendPush
import com.courier.example.databinding.ActivityMainBinding
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : CourierActivity() {

    companion object {
        const val COURIER_USER_ID = "example_user_2"
        const val COURIER_ACCESS_TOKEN = "pk_prod_X9SHD669JF400NHY56KYPTE639HH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the view to handle clicks and things
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

    private fun sendPush() {

//        Courier.sendPush(
//            authKey = accessToken,
//            userId = userId,
//            title = "This is a title",
//            body = "This is a message",
//            providers = listOf(CourierProvider.FCM),
//            onSuccess = { requestId ->
//                print(requestId)
//            },
//            onFailure = { e ->
//                print(e)
//            }
//        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestId = Courier.sendPush(
                    authKey = COURIER_ACCESS_TOKEN,
                    userId = COURIER_USER_ID,
                    title = "This is a title",
                    body = "This is a message",
                    providers = listOf(CourierProvider.FCM)
                )
                print(requestId)
            } catch (e: Exception) {
                print(e)
            }
        }

    }

    override fun onPushNotificationClicked(message: RemoteMessage) {
        print(message)
        Toast.makeText(this, "Message clicked: ${message.data["body"]}", Toast.LENGTH_LONG).show()
    }

    override fun onPushNotificationDelivered(message: RemoteMessage) {
        print(message)
        Toast.makeText(this, "Message delivered: ${message.data["body"]}", Toast.LENGTH_LONG).show()
    }

}