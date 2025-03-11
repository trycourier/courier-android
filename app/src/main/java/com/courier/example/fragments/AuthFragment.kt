package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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
import com.courier.example.AuthPreferences
import com.courier.example.Env
import com.courier.example.ExampleServer
import com.courier.example.KeyValueListItem
import com.courier.example.R
import com.courier.example.showAlert
import kotlinx.coroutines.launch

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private lateinit var authButton: Button
    private lateinit var resetButton: Button
    private lateinit var authValues: LinearLayout
    private lateinit var authPreferences: AuthPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authButton = view.findViewById(R.id.authButton)
        resetButton = view.findViewById(R.id.resetButton)
        authValues = view.findViewById(R.id.authValues)
        authPreferences = AuthPreferences(requireContext())

        refresh()
    }

    private fun refresh() {
        authButton.isEnabled = true
        resetButton.isEnabled = !Courier.shared.isUserSignedIn

        if (Courier.shared.isUserSignedIn) {
            // Clear existing views
            authValues.removeAllViews()

            // Create map of values to display
            val values = mapOf(
                "userId" to Courier.shared.userId,
                "tenantId (optional)" to Courier.shared.tenantId,
                "apiKey" to (authPreferences.apiKey ?: Env.COURIER_AUTH_KEY),
                "restUrl" to Courier.shared.client?.options?.apiUrls?.rest,
                "graphqlUrl" to Courier.shared.client?.options?.apiUrls?.graphql,
                "inboxGraphqlUrl" to Courier.shared.client?.options?.apiUrls?.inboxGraphql,
                "inboxWebSocketUrl" to Courier.shared.client?.options?.apiUrls?.inboxWebSocket,
            )

            // Add KeyValueListItem for each value
            values.forEach { (key, value) ->
                val item = KeyValueListItem(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                         bottomMargin = (8 * resources.displayMetrics.density).toInt()
                    }
                    setKeyValue(key, value ?: "NOT SET")
                }
                authValues.addView(item)
            }

            authButton.text = "Sign Out"
            authButton.setOnClickListener {
                signOut()
            }
        } else {
            authValues.removeAllViews()

            // Add single KeyValueListItem for signed out state
            val item = KeyValueListItem(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setKeyValue("Status", "Courier user not signed in")
            }
            authValues.addView(item)

            authButton.text = "Sign In"
            authButton.setOnClickListener {
                signIn()
            }
        }
    }

    private fun signIn() = lifecycleScope.launch {
        authButton.isEnabled = false
        resetButton.isEnabled = false

        try {
            val fallbackUrls = CourierClient.ApiUrls()

            val values = showAlert(
                context = requireContext(),
                title = "Enter Sign In Credentials",
                items = mapOf(
                    "userId" to (authPreferences.userId ?: ""),
                    "tenantId" to (authPreferences.tenantId ?: ""),
                    "apiKey" to (authPreferences.apiKey ?: Env.COURIER_AUTH_KEY),
                    "restUrl" to (authPreferences.restUrl ?: fallbackUrls.rest),
                    "graphqlUrl" to (authPreferences.graphqlUrl ?: fallbackUrls.graphql),
                    "inboxGraphqlUrl" to (authPreferences.inboxGraphqlUrl ?: fallbackUrls.inboxGraphql),
                    "inboxWebSocketUrl" to (authPreferences.inboxWebSocketUrl ?: fallbackUrls.inboxWebSocket),
                ),
                action = "Sign In"
            )

            val userId = values["userId"]!!
            val tenantId = values["tenantId"]!!.ifEmpty { null }
            val apiKey = values["apiKey"]!!.ifEmpty { Env.COURIER_AUTH_KEY }
            val apiUrls = CourierClient.ApiUrls(
                rest = values["restUrl"]!!.ifEmpty { fallbackUrls.rest },
                graphql = values["graphqlUrl"]!!.ifEmpty { fallbackUrls.graphql },
                inboxGraphql = values["inboxGraphqlUrl"]!!.ifEmpty { fallbackUrls.inboxGraphql },
                inboxWebSocket = values["inboxWebSocketUrl"]!!.ifEmpty { fallbackUrls.inboxWebSocket },
            )

            val jwt = ExampleServer().generateJWT(
                authKey = apiKey,
                userId = userId,
                baseUrl = apiUrls.rest
            )

            // Save values to SharedPreferences
            authPreferences.userId = userId
            authPreferences.tenantId = tenantId
            authPreferences.apiKey = apiKey
            authPreferences.saveApiUrls(apiUrls)

            Courier.shared.signIn(
                userId = userId,
                accessToken = jwt,
                tenantId = tenantId,
                apiUrls = apiUrls
            )

        } catch (e: Exception) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
        }

        refresh()
        authButton.isEnabled = true
    }

    private fun signOut() = lifecycleScope.launch {

        authButton.isEnabled = false
        resetButton.isEnabled = true

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