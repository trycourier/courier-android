package com.courier.example.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.modules.isUserSignedIn
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.android.modules.userId
import com.courier.example.Env
import com.courier.example.R
import kotlinx.coroutines.launch

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private lateinit var authButton: Button
    private lateinit var authTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authButton = view.findViewById(R.id.auth_button)
        authTextView = view.findViewById(R.id.auth_text_view)

        refresh()

    }

    private suspend fun setup() {

//        try {
//
//            val hasNotificationPermissions = requestNotificationPermission()
//            Toast.makeText(
//                this@MainActivity,
//                "Notification permissions are granted: $hasNotificationPermissions",
//                Toast.LENGTH_LONG
//            ).show()
//
//        } catch (e: Exception) {
//
//            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
//            setup()
//
//        }

    }

    private fun refresh() {

        authButton.isEnabled = true

        if (Courier.shared.isUserSignedIn) {
            authTextView.text = "Courier user signed in: ${Courier.shared.userId}"
            authButton.text = "Sign Out"
            authButton.setOnClickListener {
                signOut()
            }
        } else {
            authTextView.text = "Courier user not signed in"
            authButton.text = "Sign In"
            authButton.setOnClickListener {
                signIn()
            }
        }

    }

    private fun signIn() = lifecycleScope.launch {

        authButton.isEnabled = false

        try {

            Courier.shared.signIn(
                accessToken = Env.COURIER_ACCESS_TOKEN,
                userId = Env.COURIER_USER_ID,
                clientKey = Env.COURIER_CLIENT_KEY
            )

            refresh()

            Toast.makeText(context, "Courier user signed in", Toast.LENGTH_LONG).show()


        } catch (e: Exception) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

        }

        authButton.isEnabled = true

    }

    private fun signOut() = lifecycleScope.launch {

        authButton.isEnabled = false

        try {

            Courier.shared.signOut()

            refresh()

            Toast.makeText(context, "Courier user signed out", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

        }

        authButton.isEnabled = true

    }

}