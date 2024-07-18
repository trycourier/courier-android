package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import com.courier.android.Courier
import com.courier.android.models.CourierException
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.models.CourierPreferenceStatus
import com.courier.example.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferenceDetailFragment(private val topicId: String) : Fragment(R.layout.fragment_preference_detail) {

    private lateinit var scrollView: ScrollView
    private lateinit var loadingIndicatorContainer: LinearLayoutCompat
    private lateinit var toolbar: MaterialToolbar
    private lateinit var statusTabs: TabLayout
    private lateinit var customRoutingSwitch: SwitchCompat
    private lateinit var pushSwitch: SwitchCompat
    private lateinit var smsSwitch: SwitchCompat
    private lateinit var emailSwitch: SwitchCompat
    private lateinit var dmSwitch: SwitchCompat
    private lateinit var webhooksSwitch: SwitchCompat

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view.findViewById(R.id.scrollview)
        loadingIndicatorContainer = view.findViewById(R.id.loadingIndicatorContainer)

        toolbar = view.findViewById(R.id.toolbar)
        toolbar.title = topicId

        toolbar.setNavigationOnClickListener {
            activity?.supportFragmentManager?.popBackStackImmediate()
        }

        toolbar.setOnMenuItemClickListener { _ ->
            save()
            return@setOnMenuItemClickListener true
        }

        statusTabs = view.findViewById(R.id.statusTabs)

        customRoutingSwitch = view.findViewById(R.id.customRoutingSwitch)
        pushSwitch = view.findViewById(R.id.pushSwitch)
        smsSwitch = view.findViewById(R.id.smsSwitch)
        emailSwitch = view.findViewById(R.id.emailSwitch)
        dmSwitch = view.findViewById(R.id.dmSwitch)
        webhooksSwitch = view.findViewById(R.id.webhooksSwitch)

        load()

    }

    private fun load() = lifecycle.coroutineScope.launch(Dispatchers.Main) {

        scrollView.isVisible = false
        loadingIndicatorContainer.isVisible = true

        try {

            Courier.shared.client?.preferences?.getUserPreferenceTopic(topicId)?.let { topic ->

                // Setup tabs
                statusTabs.removeAllTabs()
                val selectedTabIndex = CourierPreferenceStatus.allCases.indexOf(topic.status)
                CourierPreferenceStatus.allCases.forEachIndexed { index, status ->

                    val tabItem = statusTabs.newTab()
                    tabItem.text = status.value
                    statusTabs.addTab(tabItem)

                    if (index == selectedTabIndex) {
                        statusTabs.selectTab(tabItem)
                    }

                }

                // Custom Routing
                customRoutingSwitch.isChecked = topic.hasCustomRouting

                // Routing
                pushSwitch.isChecked = topic.customRouting.contains(CourierPreferenceChannel.PUSH)
                smsSwitch.isChecked = topic.customRouting.contains(CourierPreferenceChannel.SMS)
                emailSwitch.isChecked = topic.customRouting.contains(CourierPreferenceChannel.EMAIL)
                dmSwitch.isChecked = topic.customRouting.contains(CourierPreferenceChannel.DIRECT_MESSAGE)
                webhooksSwitch.isChecked = topic.customRouting.contains(CourierPreferenceChannel.WEBHOOK)

            }

        } catch (e: CourierException) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

        }

        scrollView.isVisible = true
        loadingIndicatorContainer.isVisible = false

    }

    private fun save() = lifecycle.coroutineScope.launch(Dispatchers.Main) {

        scrollView.isVisible = false
        loadingIndicatorContainer.isVisible = true

        try {

            val status = CourierPreferenceStatus.allCases[statusTabs.selectedTabPosition]

            val routing = mapOf(
                CourierPreferenceChannel.PUSH to pushSwitch.isChecked,
                CourierPreferenceChannel.SMS to smsSwitch.isChecked,
                CourierPreferenceChannel.EMAIL to emailSwitch.isChecked,
                CourierPreferenceChannel.DIRECT_MESSAGE to dmSwitch.isChecked,
                CourierPreferenceChannel.WEBHOOK to webhooksSwitch.isChecked,
            )

            Courier.shared.client?.preferences?.putUserPreferenceTopic(
                topicId = topicId,
                status = status,
                hasCustomRouting = customRoutingSwitch.isChecked,
                customRouting = routing.filter { it.value }.map { it.key }
            )

            activity?.supportFragmentManager?.popBackStackImmediate()

        } catch (e: CourierException) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()

        }

        scrollView.isVisible = true
        loadingIndicatorContainer.isVisible = false

    }

}