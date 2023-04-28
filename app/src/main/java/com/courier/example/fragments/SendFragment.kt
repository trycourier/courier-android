package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.modules.sendMessage
import com.courier.example.Env
import com.courier.example.R
import kotlinx.coroutines.launch

class SendFragment: Fragment(R.layout.fragment_send) {

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

            val checkboxes = listOf(checkFcm, checkApns, checkInbox)

            val providers = checkboxes.filter { it.isChecked }.map { checkbox ->
                return@map when (checkbox.id) {
                    checkFcm.id -> CourierProvider.FCM
                    checkApns.id -> CourierProvider.APNS
                    checkInbox.id -> CourierProvider.INBOX
                    else -> CourierProvider.FCM
                }
            }

            sendButton.isEnabled = false

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

                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

            }

            sendButton.isEnabled = true

        }

    }

}