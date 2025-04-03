package com.courier.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.courier.android.Courier
import com.courier.android.activity.CourierActivity
import com.courier.android.client.CourierClient
import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxListener
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.signIn
import com.courier.android.modules.tenantId
import com.courier.android.modules.userId
import com.courier.android.utils.onPushNotificationEvent
import com.courier.example.databinding.ActivityMainBinding
import com.courier.example.fragments.AuthFragment
import com.courier.example.fragments.InboxFragment
import com.courier.example.fragments.PreferencesFragment
import com.courier.example.fragments.PushFragment
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch


class MainActivity : CourierActivity() {

    private lateinit var inboxListener: CourierInboxListener

    private val authFragment by lazy { AuthFragment() }
    private val inboxFragment by lazy { InboxFragment() }
    private val sendFragment by lazy { PushFragment() }
    private val preferenceFragment by lazy { PreferencesFragment() }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
    }

    private fun setup() = lifecycleScope.launch {

        try {

            // Fetch new jwt if needed
            Courier.shared.userId?.let { userId ->

                val jwt = ExampleServer().generateJWT(
                    authKey = Env.COURIER_AUTH_KEY, // TODO: Handle this
                    userId = userId,
                    baseUrl = CourierClient.ApiUrls().rest // TODO: Make this use the shared preference
                )

                val tenantId = Courier.shared.tenantId

                Courier.shared.signIn(
                    userId = userId,
                    tenantId = tenantId,
                    accessToken = jwt,
                )

            }

        } catch (e: CourierException) {

            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()

        }

        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        setCurrentFragment(authFragment)

        binding.bottomNavigationView.setOnItemSelectedListener {
            return@setOnItemSelectedListener when (it.itemId) {
                R.id.auth -> setCurrentFragment(authFragment)
                R.id.inbox -> setCurrentFragment(inboxFragment)
                R.id.send -> setCurrentFragment(sendFragment)
                R.id.preferences -> setCurrentFragment(preferenceFragment)
                else -> false
            }
        }

        inboxListener = Courier.shared.addInboxListener(
            onUnreadCountChanged = { count ->
                setBadge(count)
            }
        )

    }

    fun setCurrentFragment(fragment: Fragment, addToBackStack: Boolean = false): Boolean {
        supportFragmentManager.beginTransaction().let {
            it.replace(R.id.fragmentContainer, fragment)
            if (addToBackStack) {
                it.addToBackStack(null)
            }
            return@let it.commit()
        }
        return true
    }

    private fun setBadge(value: Int) {
        val badge = binding.bottomNavigationView.getOrCreateBadge(R.id.inbox)
        badge.backgroundColor = ContextCompat.getColor(this, R.color.purple_700)
        badge.badgeTextColor = ContextCompat.getColor(this, R.color.white)
        badge.maxCharacterCount = 3
        badge.verticalOffset = 2
        badge.isVisible = value > 0
        badge.number = value
    }

    override fun onDestroy() {
        super.onDestroy()
        inboxListener.remove()
    }

    override fun onPushNotificationClicked(message: RemoteMessage) {
        Log.d("Courier", message.toJsonString())
        Toast.makeText(this, "Message clicked:\n${message.data}", Toast.LENGTH_LONG).show()
    }

    override fun onPushNotificationDelivered(message: RemoteMessage) {
        Log.d("Courier", message.toJsonString())
        Toast.makeText(this, "Message delivered:\n${message.data}", Toast.LENGTH_LONG).show()
    }

}