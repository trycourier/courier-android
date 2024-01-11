package com.courier.example.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.models.CourierPreferenceStatus
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.modules.getUserPreferenceTopic
import com.courier.android.modules.getUserPreferences
import com.courier.android.modules.putUserPreferenceTopic
import com.courier.android.modules.refreshInbox
import com.courier.example.R
import com.courier.example.showAlert
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesFragment : Fragment(R.layout.fragment_preferences) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout

    private val preferencesAdapter = PreferencesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create the list
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = preferencesAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        // Handle pull to refresh
        refreshLayout = view.findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {

            Courier.shared.refreshInbox {
                refreshLayout.isRefreshing = false
            }

        }

        preferencesAdapter.onItemClick = { topic ->

            lifecycle.coroutineScope.launch(Dispatchers.Main) {

                try {

                    refreshLayout.isRefreshing = true

                    val preferenceTopic = Courier.shared.getUserPreferenceTopic(
                        topicId = topic.topicId,
                    )

                    print(preferenceTopic)

                    Courier.shared.putUserPreferenceTopic(
                        topicId = preferenceTopic.topicId,
                        status = CourierPreferenceStatus.OPTED_IN,
                        hasCustomRouting = true,
                        customRouting = getRandomChannels()
                    )

                    Toast.makeText(context, "Preference Updated", Toast.LENGTH_SHORT).show()

                    load()

                } catch (e: Exception) {

                    refreshLayout.isRefreshing = false

                    showAlert(requireContext(), "Error Updating Preferences", e.toString())

                }

            }

        }

        load()

    }

    private fun getRandomChannels(): List<CourierPreferenceChannel> {
        val channelValues = listOf(
            CourierPreferenceChannel.DIRECT_MESSAGE,
            CourierPreferenceChannel.EMAIL,
            CourierPreferenceChannel.PUSH,
            CourierPreferenceChannel.SMS,
            CourierPreferenceChannel.WEBHOOK
        )

        val randomCount = (0..channelValues.size).random()
        val randomChannels = mutableListOf<CourierPreferenceChannel>()

        while (randomChannels.size < randomCount) {
            val randomChannel = channelValues.random()
            if (randomChannel !in randomChannels) {
                randomChannels.add(randomChannel)
            }
        }

        return randomChannels
    }

    private fun load() {

        refreshLayout.isRefreshing = true

        Courier.shared.getUserPreferences(
            onSuccess = { preferences ->
                preferencesAdapter.topics = preferences.items
                refreshLayout.isRefreshing = false
            },
            onFailure = { error ->
                print(error)
                preferencesAdapter.topics = emptyList()
                refreshLayout.isRefreshing = false
            }
        )

    }

}

class PreferencesAdapter : RecyclerView.Adapter<MessageItemViewHolder>() {

    var topics: List<CourierPreferenceTopic> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemClick: ((CourierPreferenceTopic) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageItemViewHolder, position: Int) {
        val topic = topics[position]
        holder.textView.text = Gson().toJson(topic).toString()
        holder.textView.setOnClickListener {
            onItemClick?.invoke(topic)
        }
    }

    override fun getItemCount(): Int {
        return topics.size
    }

}