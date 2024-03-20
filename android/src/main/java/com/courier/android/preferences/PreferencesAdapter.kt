package com.courier.android.preferences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.R
import com.courier.android.inbox.CourierInboxTheme
import com.courier.android.models.CourierPreferenceTopic
import com.google.gson.Gson

internal class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val container: ConstraintLayout
    private val textView: TextView

    private var topic: CourierPreferenceTopic? = null

    var onTopicClick: ((CourierPreferenceTopic) -> Unit)? = null

    init {
        container = itemView.findViewById(R.id.container)
        textView = itemView.findViewById(R.id.textView)
    }

    fun setTopic(theme: CourierInboxTheme, topic: CourierPreferenceTopic) {

        this.topic = topic

        textView.text = Gson().toJson(topic).toString()

        // Container
        container.setOnClickListener {
            onTopicClick?.invoke(topic)
        }

    }

}

internal class PreferencesAdapter(
    internal var theme: CourierInboxTheme,
    internal var topics: List<CourierPreferenceTopic>,
    private val onTopicClick: (CourierPreferenceTopic, Int) -> Unit,
) : RecyclerView.Adapter<TopicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_preferences_list_item, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {

        holder.setTopic(
            theme = theme,
            topic = topics[position]
        )

        holder.onTopicClick = { topic ->
            onTopicClick.invoke(topic, position)
        }

    }

    override fun getItemCount(): Int = topics.size

}