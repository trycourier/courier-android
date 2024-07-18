package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.models.CourierException
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.modules.refreshInbox
import com.courier.example.MainActivity
import com.courier.example.R
import com.courier.example.fragments.inbox.MessageItemViewHolder
import com.courier.example.toJson
import kotlinx.coroutines.launch

class CustomPreferencesFragment : Fragment(R.layout.fragment_custom_preferences) {

    private lateinit var stateTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout

    private val preferencesAdapter = PreferencesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stateTextView = view.findViewById(R.id.stateTextView)

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
            (activity as MainActivity).setCurrentFragment(
                fragment = PreferenceDetailFragment(topicId = topic.topicId),
                addToBackStack = true,
            )
        }

        load()

    }

    private fun load() = lifecycle.coroutineScope.launch {

        val client = Courier.shared.client

        if (client == null) {
            val e = CourierException.userNotFound
            stateTextView.text = e.message
            stateTextView.isVisible = true
            return@launch
        }

        refreshLayout.isRefreshing = true

        try {

            val preferences = client.preferences.getUserPreferences()
            preferencesAdapter.topics = preferences.items
            refreshLayout.isRefreshing = false
            stateTextView.isVisible = false

        } catch (e: Exception) {

            print(e)
            preferencesAdapter.topics = emptyList()
            refreshLayout.isRefreshing = false
            stateTextView.isVisible = true
            stateTextView.text = e.message

        }

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
        holder.textView.text = topic.toJson()
        holder.textView.setOnClickListener {
            onItemClick?.invoke(topic)
        }
    }

    override fun getItemCount(): Int {
        return topics.size
    }

}