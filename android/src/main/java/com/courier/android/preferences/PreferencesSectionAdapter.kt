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

internal class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val container: ConstraintLayout
    private val textView: TextView

    private var topic: CourierPreferenceTopic? = null

    init {
        container = itemView.findViewById(R.id.container)
        textView = itemView.findViewById(R.id.textView)
    }

    fun setSectionTopic(theme: CourierInboxTheme, topic: CourierPreferenceTopic) {

        this.topic = topic

        textView.text = topic.sectionName

    }

}

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

internal class PreferencesSectionAdapter(
    internal var theme: CourierInboxTheme,
    internal val section: CourierPreferenceTopic,
    internal var topics: MutableList<CourierPreferenceTopic>,
    private val onTopicClick: (CourierPreferenceTopic, Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val SECTION_VIEW = 0
        private const val ITEM_VIEW = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) SECTION_VIEW else ITEM_VIEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        if (viewType == SECTION_VIEW) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_preferences_section_item, parent, false)
            return SectionHeaderViewHolder(view)
        }

        val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_preferences_topic_item, parent, false)
        return TopicViewHolder(view)

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (position == 0) {

            // Show the section header
            (holder as SectionHeaderViewHolder).setSectionTopic(
                theme = theme,
                topic = section
            )

            return
        }

        // Show the topic item
        (holder as TopicViewHolder).let { viewHolder ->

            val index = position - 1

            viewHolder.setTopic(
                theme = theme,
                topic = topics[index]
            )

            viewHolder.onTopicClick = { topic ->
                onTopicClick.invoke(topic, index)
            }

        }

    }

    override fun getItemCount(): Int = topics.size + 1

}