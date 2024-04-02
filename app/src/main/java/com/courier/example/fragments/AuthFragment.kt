package com.courier.example.fragments

import android.os.Bundle
import android.view.View
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
import com.courier.example.ExampleServer
import com.courier.example.R
import com.courier.example.showAlert
import kotlinx.coroutines.launch

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private lateinit var authButton: Button
    private lateinit var authTextView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authButton = view.findViewById(R.id.authButton)
        authTextView = view.findViewById(R.id.authTextView)

        refresh()

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

            val values = showAlert(
                context = requireContext(),
                title = "Enter a user id",
                items = mapOf(
                    "userId" to ""
                )
            )

            val userId = values["userId"]!!

            val jwt = ExampleServer().generateJWT(
                authKey = Env.COURIER_AUTH_KEY,
                userId = userId
            )

            Courier.shared.signIn(
                userId = userId,
                accessToken = jwt,
            )

        } catch (e: Exception) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

        }

        refresh()

        authButton.isEnabled = true

    }

    private fun signOut() = lifecycleScope.launch {

        authButton.isEnabled = false

        try {

            Courier.shared.signOut()

            Toast.makeText(context, "Courier user signed out", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

        }

        refresh()

        authButton.isEnabled = true

    }

}