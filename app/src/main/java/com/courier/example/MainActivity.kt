package com.courier.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.activity.CourierActivity
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.inboxBrandId
import com.courier.example.databinding.ActivityMainBinding
import com.courier.example.fragments.*
import com.google.firebase.messaging.RemoteMessage


class MainActivity : CourierActivity() {

    private lateinit var inboxListener: CourierInboxListener

    private val authFragment by lazy { AuthFragment() }
    private val prebuiltInboxFragment by lazy { PrebuiltInboxFragment() }
    private val styledInboxFragment by lazy { StyledInboxFragment() }
    private val customInboxFragment by lazy { CustomInboxFragment() }
    private val sendFragment by lazy { SendFragment() }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        setCurrentFragment(authFragment)

        binding.bottomNavigationView.setOnItemSelectedListener {
            return@setOnItemSelectedListener when (it.itemId) {
                R.id.auth -> setCurrentFragment(authFragment)
                R.id.prebuiltInbox -> setCurrentFragment(prebuiltInboxFragment)
                R.id.styledInbox -> setCurrentFragment(styledInboxFragment)
                R.id.customInbox -> setCurrentFragment(customInboxFragment)
                R.id.send -> setCurrentFragment(sendFragment)
                else -> false
            }
        }

        // Sets the brand at a global level -- Optional
//        Courier.shared.inboxBrandId = Env.COURIER_BRAND_ID

        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {
                setBadge(0)
            },
            onError = {
                setBadge(0)
            },
            onMessagesChanged = { _, unreadMessageCount, _, _ ->
                setBadge(unreadMessageCount)
            }
        )

    }

    private fun setCurrentFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction().let {
            it.replace(R.id.fragmentContainer, fragment)
            return@let it.commit()
        }
        return true
    }

    private fun setBadge(value: Int) {

        val bottomNav = binding.bottomNavigationView

        listOf(R.id.prebuiltInbox, R.id.styledInbox, R.id.customInbox).forEach { itemId ->
            val badge = bottomNav.getOrCreateBadge(itemId)
            badge.backgroundColor = ContextCompat.getColor(this, R.color.purple_700)
            badge.badgeTextColor = ContextCompat.getColor(this, R.color.white)
            badge.maxCharacterCount = 3
            badge.verticalOffset = 2
            badge.isVisible = value > 0
            badge.number = value
        }

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