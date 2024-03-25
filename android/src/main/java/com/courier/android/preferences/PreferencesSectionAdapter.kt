package com.courier.android.preferences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.R
import com.courier.android.models.CourierPreferenceStatus
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.utils.isDarkModeOn
import com.courier.android.utils.setCourierFont

internal class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val textView: TextView

    init {
        textView = itemView.findViewById(R.id.textView)
    }

    fun setSectionTopic(theme: CourierPreferencesTheme, topic: CourierPreferenceTopic) {

        val res = if (isDarkModeOn(itemView.context)) android.R.color.white else android.R.color.black
        val fallbackColor = ContextCompat.getColor(itemView.context, res)

        textView.text = topic.sectionName
        textView.setCourierFont(font = theme.sectionTitleFont, fallbackColor = fallbackColor)

    }

}

internal class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val container: LinearLayoutCompat
    private val titleTextView: TextView
    private val subtitleTextView: TextView

    var onTopicClick: ((CourierPreferenceTopic) -> Unit)? = null

    init {
        container = itemView.findViewById(R.id.container)
        titleTextView = itemView.findViewById(R.id.titleTextView)
        subtitleTextView = itemView.findViewById(R.id.subtitleTextView)
    }

    fun setTopic(theme: CourierPreferencesTheme, mode: CourierPreferences.Mode, topic: CourierPreferenceTopic) {

        titleTextView.text = topic.topicName

        when (mode) {
            is CourierPreferences.Mode.Channels -> {
                subtitleTextView.text = if (topic.status == CourierPreferenceStatus.OPTED_OUT) {
                    "Off"
                } else if (topic.status == CourierPreferenceStatus.REQUIRED && topic.customRouting.isEmpty()) {
                    "On: " + mode.channels.joinToString(separator = ", ") { it.title }
                } else if (topic.status == CourierPreferenceStatus.OPTED_IN && topic.customRouting.isEmpty()) {
                    "On: " + mode.channels.joinToString(separator = ", ") { it.title }
                } else {
                    "On: " + topic.customRouting.joinToString(separator = ", ") { it.title }
                }
            }
            CourierPreferences.Mode.Topic -> {
                subtitleTextView.text = topic.status.title
            }
        }

        val res = if (isDarkModeOn(itemView.context)) android.R.color.white else android.R.color.black
        val fallbackColor = ContextCompat.getColor(itemView.context, res)

        // Set the font
        titleTextView.setCourierFont(font = theme.topicTitleFont, fallbackColor = fallbackColor)
        subtitleTextView.setCourierFont(font = theme.topicSubtitleFont, fallbackColor = fallbackColor)

        // Container
        container.setOnClickListener {
            onTopicClick?.invoke(topic)
        }

    }

}

internal class PreferencesSectionAdapter(
    internal var theme: CourierPreferencesTheme,
    internal val mode: CourierPreferences.Mode,
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
                mode = mode,
                topic = topics[index]
            )

            viewHolder.onTopicClick = { topic ->
                onTopicClick.invoke(topic, index)
            }

        }

    }

    override fun getItemCount(): Int = topics.size + 1

}