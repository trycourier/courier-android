package com.courier.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.activity.CourierActivity
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.remove
import com.courier.android.modules.*
import com.courier.example.databinding.ActivityMainBinding
import com.courier.example.fragments.AuthFragment
import com.courier.example.fragments.CustomInboxFragment
import com.courier.example.fragments.SendFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.RemoteMessage


class MainActivity : CourierActivity() {

    private lateinit var inboxListener: CourierInboxListener

    private val authFragment = AuthFragment()
    private val customInboxFragment = CustomInboxFragment()
    private val sendFragment = SendFragment()

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
                R.id.customInbox -> setCurrentFragment(customInboxFragment)
                R.id.send -> setCurrentFragment(sendFragment)
                else -> false
            }
        }

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

        listOf(R.id.customInbox).forEach { itemId ->
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