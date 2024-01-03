package com.courier.example.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.modules.getUserPreferences
import com.courier.android.modules.refreshInbox
import com.courier.example.R
import com.google.gson.Gson

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

        load()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageItemViewHolder, position: Int) {
        val topic = topics[position]
        holder.textView.text = Gson().toJson(topic).toString()
        holder.textView.setOnClickListener {
//            if (isRead) inboxMessage.markAsUnread() else inboxMessage.markAsRead()
        }
    }

    override fun getItemCount(): Int {
        return topics.size
    }

}