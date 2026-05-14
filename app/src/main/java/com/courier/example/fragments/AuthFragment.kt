package com.courier.example.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierAuthenticationListener
import com.courier.android.models.remove
import com.courier.android.modules.addAuthenticationListener
import com.courier.android.modules.signIn
import com.courier.android.modules.signOut
import com.courier.android.modules.userId
import com.courier.example.AuthPreferences
import com.courier.example.CourierEnvironment
import com.courier.example.Env
import com.courier.example.ExampleServer
import com.courier.example.KeyValueListItem
import com.courier.example.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AuthFragment : Fragment(R.layout.fragment_auth) {

    private lateinit var authValues: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var authPreferences: AuthPreferences

    private var saveMenuItem: MenuItem? = null
    private var authListener: CourierAuthenticationListener? = null
    private var selectedEnvironment: CourierEnvironment = CourierEnvironment.PRODUCTION

    private val options: MutableList<Pair<String, String>> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authValues = view.findViewById(R.id.authValues)
        toolbar = view.findViewById(R.id.toolbar)
        authPreferences = AuthPreferences(requireContext())

        initOptions()
        setupToolbar()
        refresh()

        authListener = Courier.shared.addAuthenticationListener { _ ->
            activity?.runOnUiThread {
                updateSaveEnabled()
                refresh()
            }
        }
    }

    private fun initOptions() {
        val fallbackUrls = CourierClient.ApiUrls()
        val envName = authPreferences.environment ?: CourierEnvironment.PRODUCTION.label
        selectedEnvironment = CourierEnvironment.fromLabel(envName)

        options.clear()
        options.addAll(listOf(
            "Environment" to selectedEnvironment.label,
            "User ID" to (authPreferences.userId ?: ""),
            "Tenant ID (Optional)" to (authPreferences.tenantId ?: ""),
            "API Key" to (authPreferences.apiKey ?: Env.COURIER_AUTH_KEY),
            "REST URL" to (authPreferences.restUrl ?: fallbackUrls.rest),
            "GraphQL URL" to (authPreferences.graphqlUrl ?: fallbackUrls.graphql),
            "Inbox GraphQL URL" to (authPreferences.inboxGraphqlUrl ?: fallbackUrls.inboxGraphql),
            "Inbox WebSocket" to (authPreferences.inboxWebSocketUrl ?: fallbackUrls.inboxWebSocket),
        ))
    }

    private fun setupToolbar() {
        toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_auth, menu)
                saveMenuItem = menu.findItem(R.id.action_save)
                updateSaveEnabled()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> {
                        performSave()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun updateSaveEnabled() {
        saveMenuItem?.isEnabled = options.getOrNull(1)?.second?.isNotEmpty() == true
    }

    private fun setSaving(isSaving: Boolean) {
        if (isSaving) {
            saveMenuItem?.isEnabled = false
            saveMenuItem?.actionView = ProgressBar(requireContext()).apply {
                val size = (24 * resources.displayMetrics.density).toInt()
                layoutParams = android.widget.FrameLayout.LayoutParams(size, size)
                isIndeterminate = true
            }
        } else {
            saveMenuItem?.actionView = null
            updateSaveEnabled()
        }
    }

    private fun performSave() {
        lifecycleScope.launch {
            setSaving(true)

            try {
                if (Courier.shared.userId != null) {
                    Courier.shared.signOut()
                }
                performSignIn()
            } catch (e: Exception) {
                showErrorAlert(e.toString())
            }

            setSaving(false)
        }
    }

    private suspend fun performSignIn() {
        val userId = options[1].second
        val tenantId = options[2].second.ifEmpty { null }
        val apiKey = options[3].second

        if (userId.isEmpty()) {
            Courier.shared.signOut()
            return
        }

        val jwt = ExampleServer().generateJWT(
            authKey = apiKey,
            userId = userId,
            baseUrl = options[4].second
        )

        Courier.shared.signIn(
            userId = userId,
            tenantId = tenantId,
            accessToken = jwt,
            apiUrls = CourierClient.ApiUrls(
                rest = options[4].second,
                graphql = options[5].second,
                inboxGraphql = options[6].second,
                inboxWebSocket = options[7].second,
            )
        )
    }

    private fun refresh() {
        authValues.removeAllViews()

        options.forEachIndexed { index, (key, value) ->
            val displayValue = value.ifEmpty { "NOT SET" }
            val item = KeyValueListItem(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setKeyValue(key, displayValue)

                when {
                    index == 0 -> setAccessoryIcon(R.drawable.ic_edit)
                    index >= 4 && selectedEnvironment != CourierEnvironment.CUSTOM -> setAccessoryIcon(R.drawable.ic_copy)
                    else -> setAccessoryIcon(null)
                }

                setOnClickListener { onRowTapped(index) }
            }
            authValues.addView(item)
        }
    }

    private fun onRowTapped(row: Int) {
        when {
            row == 0 -> showEnvironmentPicker()
            row in 1..3 -> showEditDialog(row)
            row >= 4 && selectedEnvironment != CourierEnvironment.CUSTOM -> {
                copyToClipboard(options[row].second)
            }
            row >= 4 -> showEditDialog(row)
        }
    }

    private fun showEnvironmentPicker() {
        val environments = CourierEnvironment.values()
        val labels = environments.map { it.label }.toTypedArray()
        val checkedIndex = environments.indexOf(selectedEnvironment)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Environment")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                dialog.dismiss()
                applyEnvironment(environments[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyEnvironment(env: CourierEnvironment) {
        selectedEnvironment = env
        options[0] = options[0].first to env.label
        authPreferences.environment = env.label

        env.urls()?.let { urls ->
            options[4] = options[4].first to urls.rest
            options[5] = options[5].first to urls.graphql
            options[6] = options[6].first to urls.inboxGraphql
            options[7] = options[7].first to urls.inboxWebSocket

            authPreferences.saveApiUrls(urls)
        }

        refresh()
    }

    private fun showEditDialog(row: Int) {
        val (title, currentValue) = options[row]

        val editText = EditText(requireContext()).apply {
            setText(currentValue)
            hint = title
            setSingleLine()
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(container)
            .setNeutralButton("Copy") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    copyToClipboard(text)
                }
            }
            .setPositiveButton("Update") { _, _ ->
                applyEdit(row, editText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun applyEdit(row: Int, value: String) {
        options[row] = options[row].first to value

        when (row) {
            1 -> {
                authPreferences.userId = value
                updateSaveEnabled()
            }
            2 -> authPreferences.tenantId = value
            3 -> authPreferences.apiKey = value
            4 -> authPreferences.restUrl = value
            5 -> authPreferences.graphqlUrl = value
            6 -> authPreferences.inboxGraphqlUrl = value
            7 -> authPreferences.inboxWebSocketUrl = value
        }

        if (row >= 4 && selectedEnvironment != CourierEnvironment.CUSTOM) {
            selectedEnvironment = CourierEnvironment.CUSTOM
            options[0] = options[0].first to CourierEnvironment.CUSTOM.label
            authPreferences.environment = CourierEnvironment.CUSTOM.label
        }

        refresh()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Courier", text))
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showErrorAlert(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        authListener?.remove()
        authListener = null
    }
}
