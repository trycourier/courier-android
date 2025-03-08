package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.modules.isUserSignedIn
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.android.modules.tenantId
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

            val values = mapOf(
                "userId" to Courier.shared.userId,
                "tenantId" to Courier.shared.tenantId,
                "restUrl" to Courier.shared.client?.options?.apiUrls?.rest,
                "graphqlUrl" to Courier.shared.client?.options?.apiUrls?.graphql,
                "inboxGraphqlUrl" to Courier.shared.client?.options?.apiUrls?.inboxGraphql,
                "inboxWebSocketUrl" to Courier.shared.client?.options?.apiUrls?.inboxWebSocket,
            )

            authTextView.text = values.map { "${it.key}: ${it.value}" }.joinToString("\n")
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

            val fallbackUrls = CourierClient.ApiUrls()

            val values = showAlert(
                context = requireContext(),
                title = "Enter Sign In Credentials",
                items = mapOf(
                    "userId" to "",
                    "tenantId" to "",
                    "apiKey" to Env.COURIER_AUTH_KEY,
                    "restUrl" to fallbackUrls.rest,
                    "graphqlUrl" to fallbackUrls.graphql,
                    "inboxGraphqlUrl" to fallbackUrls.inboxGraphql,
                    "inboxWebSocketUrl" to fallbackUrls.inboxWebSocket,
                )
            )

            val userId = values["userId"]!!
            val tenantId = values["tenantId"]!!.ifEmpty { null }

            val jwt = ExampleServer().generateJWT(
                authKey = values["apiKey"]!!.ifEmpty { Env.COURIER_AUTH_KEY },
                userId = userId,
                baseUrl = values["restUrl"]!!.ifEmpty { fallbackUrls.rest }
            )

            Courier.shared.signIn(
                userId = userId,
                accessToken = jwt,
                tenantId = tenantId,
                apiUrls = CourierClient.ApiUrls(
                    rest = values["restUrl"]!!.ifEmpty { fallbackUrls.rest },
                    graphql = values["graphqlUrl"]!!.ifEmpty { fallbackUrls.graphql },
                    inboxGraphql = values["inboxGraphqlUrl"]!!.ifEmpty { fallbackUrls.inboxGraphql },
                    inboxWebSocket = values["inboxWebSocketUrl"]!!.ifEmpty { fallbackUrls.inboxWebSocket },
                )
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