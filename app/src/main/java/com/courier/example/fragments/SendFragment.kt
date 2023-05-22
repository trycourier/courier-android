package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.*
import com.courier.android.modules.sendMessage
import com.courier.example.Env
import com.courier.example.R
import kotlinx.coroutines.launch
import kotlin.random.Random

class SendFragment : Fragment(R.layout.fragment_send) {

    private lateinit var checkFcm: CheckBox
    private lateinit var checkApns: CheckBox
    private lateinit var checkInbox: CheckBox
    private lateinit var sendButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkFcm = view.findViewById(R.id.checkFcm)
        checkApns = view.findViewById(R.id.checkApns)
        checkInbox = view.findViewById(R.id.checkInbox)
        sendButton = view.findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            sendPush()
        }

    }

    private fun sendPush() {

        lifecycleScope.launch {

            val titles = listOf(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod",
                "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore ",
                "Ullamco laboris nisi ut aliquip ex ea commodo consequat nisi ut aliquip ex ea commodo consequat duis aute irure dolor",
                "Lorem qui officia deserunt mollit anim id est laborum."
            )

            val messages = listOf(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco",
                "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                "Lorem ipsum dolor sit amet"
            )

            val title = titles.random()
            val body = messages.random()

            val channels = mutableListOf<CourierChannel>()

            if (checkFcm.isChecked) {

                val channel = FirebaseCloudMessagingChannel(
                    fcmData = mapOf(
                        "FCM_CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                    ),
                    aps = mapOf(
                        "sound" to "ping.aiff",
                        "badge" to 123,
                        "APNS_CUSTOM_NUMBER" to 456,
                        "APNS_CUSTOM_BOOLEAN" to true,
                        "APNS_CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                    )
                )

                channels.add(channel)

            }

            if (checkApns.isChecked) {

                val channel = ApplePushNotificationsServiceChannel(
                    aps = mapOf(
                        "alert" to mapOf(
                            "title" to title,
                            "body" to body
                        ),
                        "sound" to "ping.aiff",
                        "badge" to 123,
                        "CUSTOM_NUMBER" to 456,
                        "CUSTOM_BOOLEAN" to true,
                        "CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                    )
                )

                channels.add(channel)

            }

            if (checkInbox.isChecked) {

                val buttonCount = Random.nextInt(0, 2)

                val elements = mutableListOf<CourierElement>()

                for (i in 0..buttonCount) {

                    val element = CourierElement(
                        type = "action",
                        content = "Button ${i + 1}",
                        data = mapOf(
                            "CUSTOM_KEY" to "YOUR_CUSTOM_VALUE"
                        )
                    )

                    elements.add(element)

                }

                val channel = CourierInboxChannel(
                    elements = elements,
                    data = mapOf(
                        "CUSTOM_INBOX_MESSAGE_KEY" to "YOUR_CUSTOM_VALUE"
                    )
                )

                channels.add(channel)

            }

            sendButton.isEnabled = false

            try {

                val userId = Env.COURIER_USER_ID

                Courier.shared.sendMessage(
                    authKey = Env.COURIER_AUTH_KEY,
                    userIds = listOf(userId),
                    title = title,
                    body = body,
                    channels = channels,
                )

            } catch (e: Exception) {

                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

            }

            sendButton.isEnabled = true

        }

    }

}