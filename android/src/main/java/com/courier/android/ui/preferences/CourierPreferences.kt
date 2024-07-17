package com.courier.android.ui.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.R
import com.courier.android.client.error
import com.courier.android.client.log
import com.courier.android.models.CourierException
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.models.CourierPreferenceStatus
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.ui.bar.CourierBar
import com.courier.android.ui.infoview.CourierInfoView
import com.courier.android.utils.isDarkMode
import com.courier.android.utils.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourierPreferences @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class State(var title: String? = null) {
        LOADING, ERROR, CONTENT, EMPTY
    }

    private var state = State.LOADING
        set(value) {

            coroutineScope.launch(Dispatchers.Main) {

                field = value

                when (field) {
                    State.LOADING -> {
                        refreshLayout.isVisible = false
                        infoView.isVisible = false
                        loadingIndicator.isVisible = true
                    }
                    State.ERROR -> {
                        refreshLayout.isVisible = false
                        infoView.isVisible = true
                        infoView.setTitle(field.title)
                        loadingIndicator.isVisible = false
                    }
                    State.CONTENT -> {
                        refreshLayout.isVisible = true
                        infoView.isVisible = false
                        infoView.setTitle(null)
                        loadingIndicator.isVisible = false
                    }
                    State.EMPTY -> {
                        refreshLayout.isVisible = false
                        infoView.isVisible = true
                        infoView.setTitle(field.title)
                        loadingIndicator.isVisible = false
                    }
                }

            }

        }

    sealed class Mode {
        object Topic : Mode()
        data class Channels(val channels: List<CourierPreferenceChannel>) : Mode()
    }

    var mode: Mode = Mode.Channels(CourierPreferenceChannel.allCases)

    var lightTheme: CourierPreferencesTheme = CourierPreferencesTheme.DEFAULT_LIGHT
        set(value) {
            if (field != value) {
                field = value
                refreshTheme()
            }
        }

    var darkTheme: CourierPreferencesTheme = CourierPreferencesTheme.DEFAULT_DARK
        set(value) {
            if (field != value) {
                field = value
                refreshTheme()
            }
        }

    private var theme: CourierPreferencesTheme = CourierPreferencesTheme.DEFAULT_LIGHT
        @SuppressLint("NotifyDataSetChanged")
        set(value) {

            // Update the theme
            field = value

            reloadViews()

            // Update the themes
            preferencesAdapter.adapters.forEach {
                val adapter = it as PreferencesSectionAdapter
                adapter.theme = theme
            }

            // Reload the adapters
            preferencesAdapter.notifyDataSetChanged()

        }

    var onError: ((CourierException) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    private fun reloadViews() {

        // Divider line
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }

        theme.topicDividerItemDecoration?.let {
            recyclerView.addItemDecoration(it)
        }

        infoView.setTheme(theme)

        // Loading
        theme.getLoadingColor()?.let {
            loadingIndicator.indeterminateTintList = ColorStateList.valueOf(it)
            refreshLayout.setColorSchemeColors(it)
        }

        // Handle bar visibility
        courierBar.setBrand(theme.brand)

    }

    lateinit var recyclerView: RecyclerView
        private set

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var infoView: CourierInfoView
    private lateinit var courierBar: CourierBar
    private lateinit var loadingIndicator: ProgressBar

    private var onScrollPreferences: ((Int) -> Unit)? = null

    private var preferencesAdapter = ConcatAdapter()

    init {
        View.inflate(context, R.layout.courier_preferences, this)
        setup()
        refreshTheme()
    }

    private fun refreshTheme() {
        theme = if (context.isDarkMode) darkTheme else lightTheme
    }

    private fun setup() {

        // Loading
        loadingIndicator = findViewById(R.id.loadingIndicator)

        // Info View
        infoView = findViewById(R.id.infoView)

        // Courier Bar
        courierBar = findViewById(R.id.courierBar)

        // Create the list
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->

            // Get the current offset if needed
            onScrollPreferences?.let {
                val offsetDp = recyclerView.computeVerticalScrollOffset().pxToDp
                it.invoke(offsetDp)
            }

        }

        // Handle pull to refresh
        refreshLayout = findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            refresh()
        }

        state = State.LOADING

        refresh()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refresh() = coroutineScope.launch(Dispatchers.Main) {

        val client = Courier.shared.client

        if (client == null) {
            val e = CourierException.userNotFound
            onError?.invoke(e)
            state = State.ERROR.apply { title = e.message }
            refreshLayout.isRefreshing = false
            return@launch
        }

        // Get the brand
        theme.getBrandIfNeeded()

        // Get the preferences
        try {

            val preferences = client.preferences.getUserPreferences()

            val sections = mutableListOf<PreferencesSectionAdapter>()

            preferences.items.forEach { topic ->

                val sectionId = topic.sectionId

                // Find the section index
                val sectionIndex = sections.indexOfFirst { it.section.sectionId == sectionId }

                if (sectionIndex == -1) {

                    val i = sections.size

                    val newAdapter = PreferencesSectionAdapter(
                        theme = theme,
                        mode = mode,
                        section = topic,
                        topics = mutableListOf(topic),
                        onTopicClick = { preferenceTopic, topicIndex ->
                            presentSheetForTopic(theme, preferenceTopic, Pair(i, topicIndex))
                        }
                    )

                    sections.add(newAdapter)

                } else {

                    sections[sectionIndex].topics.add(topic)

                }

            }

            preferencesAdapter = ConcatAdapter(sections)
            recyclerView.adapter = preferencesAdapter

            state = if (preferences.items.isEmpty()) State.EMPTY.apply { title = "No preferences found" } else State.CONTENT
            refreshLayout.isRefreshing = false

        } catch (e: CourierException) {

            onError?.invoke(e)

            state = State.ERROR.apply { title = e.message }
            refreshLayout.isRefreshing = false

        }

        reloadViews()

    }

    private fun presentSheetForTopic(theme: CourierPreferencesTheme, topic: CourierPreferenceTopic, path: Pair<Int, Int>) {

        val items = mutableListOf<CourierSheetItem>()

        when (mode) {
            is Mode.Topic -> {
                val isRequired = topic.status == CourierPreferenceStatus.REQUIRED
                var isOn = true
                if (!isRequired) {
                    isOn = topic.status != CourierPreferenceStatus.OPTED_OUT
                }
                val item = CourierSheetItem(
                    title = "Receive Notifications",
                    isOn = isOn,
                    isDisabled = isRequired,
                    data = null
                )
                items.add(item)
            }
            is Mode.Channels -> {
                val availableChannels = (mode as Mode.Channels).channels
                items.addAll(availableChannels.map { channel ->
                    val isRequired = topic.status == CourierPreferenceStatus.REQUIRED
                    val isOn = if (topic.customRouting.isEmpty()) {
                        topic.status != CourierPreferenceStatus.OPTED_OUT
                    } else {
                        topic.customRouting.any { it == channel }
                    }
                    return@map CourierSheetItem(
                        title = channel.title,
                        isOn = isOn,
                        isDisabled = isRequired,
                        data = channel
                    )
                })
            }
        }

        val sheet = PreferenceTopicBottomSheet(
            theme = theme,
            topic = topic,
            items = items,
            onDismiss = { newItems ->
                handleChangeForMode(mode, topic, newItems, path)
            },
            onError = { error ->
                onError?.invoke(error)
            }
        )

        sheet.show(context)

    }

    private fun handleChangeForMode(mode: Mode, originalTopic: CourierPreferenceTopic, items: List<CourierSheetItem>, path: Pair<Int, Int>) {

        if (originalTopic.defaultStatus == CourierPreferenceStatus.REQUIRED && originalTopic.status == CourierPreferenceStatus.REQUIRED) {
            return
        }

        when (mode) {
            is Mode.Topic -> {
                val selectedItems = items.filter { it.isOn }
                val isSelected = selectedItems.isNotEmpty()

                if (originalTopic.status == CourierPreferenceStatus.OPTED_IN && isSelected) {
                    return
                }

                if (originalTopic.status == CourierPreferenceStatus.OPTED_OUT && !isSelected) {
                    return
                }

                val newStatus = if (isSelected) CourierPreferenceStatus.OPTED_IN else CourierPreferenceStatus.OPTED_OUT

                val newTopic = CourierPreferenceTopic(
                    defaultStatus = originalTopic.defaultStatus,
                    hasCustomRouting = false,
                    custom_routing = emptyList(),
                    status = newStatus,
                    topicId = originalTopic.topicId,
                    topicName = originalTopic.topicName,
                    sectionName = originalTopic.sectionName,
                    sectionId = originalTopic.sectionId
                )

                // Unchanged
                if (newTopic == originalTopic) {
                    return
                }

                setTopicAtPath(topic = newTopic, path = path)

                // Update the Topic
                coroutineScope.launch {
                    val client = Courier.shared.client
                    try {
                        client?.preferences?.putUserPreferenceTopic(
                            topicId = originalTopic.topicId,
                            status = newStatus,
                            hasCustomRouting = newTopic.hasCustomRouting,
                            customRouting = newTopic.customRouting
                        )
                        client?.options?.log("Topic updated: ${originalTopic.topicId}")
                    } catch (error: Exception) {
                        client?.options?.error(error.message)
                        onError?.invoke(error as CourierException)
                        setTopicAtPath(topic = originalTopic, path = path)
                    }
                }
            }
            is Mode.Channels -> {
                val selectedItems = items.filter { it.isOn }.map { it.data as CourierPreferenceChannel }

                val newStatus = if (selectedItems.isEmpty()) CourierPreferenceStatus.OPTED_OUT else CourierPreferenceStatus.OPTED_IN

                val hasCustomRouting: Boolean
                val customRouting: List<CourierPreferenceChannel>
                val areAllSelected = selectedItems.size == items.size

                if (areAllSelected && originalTopic.defaultStatus == CourierPreferenceStatus.OPTED_IN) {
                    hasCustomRouting = false
                    customRouting = emptyList()
                } else if (selectedItems.isEmpty() && originalTopic.defaultStatus == CourierPreferenceStatus.OPTED_OUT) {
                    hasCustomRouting = false
                    customRouting = emptyList()
                } else {
                    hasCustomRouting = true
                    customRouting = selectedItems
                }

                val newTopic = CourierPreferenceTopic(
                    defaultStatus = originalTopic.defaultStatus,
                    hasCustomRouting = hasCustomRouting,
                    custom_routing = customRouting.map { it.value },
                    status = newStatus,
                    topicId = originalTopic.topicId,
                    topicName = originalTopic.topicName,
                    sectionName = originalTopic.sectionName,
                    sectionId = originalTopic.sectionId
                )

                // Unchanged
                if (newTopic == originalTopic) {
                    return
                }

                setTopicAtPath(topic = newTopic, path = path)

                // Update the Topic
                coroutineScope.launch {
                    val client = Courier.shared.client
                    try {
                        client?.preferences?.putUserPreferenceTopic(
                            topicId = originalTopic.topicId,
                            status = newStatus,
                            hasCustomRouting = hasCustomRouting,
                            customRouting = customRouting
                        )
                        client?.options?.log("Topic updated: ${originalTopic.topicId}")
                    } catch (error: Exception) {
                        client?.options?.error(error.message)
                        onError?.invoke(error as CourierException)
                        setTopicAtPath(topic = originalTopic, path = path)
                    }
                }
            }
        }
    }

    private fun setTopicAtPath(topic: CourierPreferenceTopic, path: Pair<Int, Int>) {
        val adapter = preferencesAdapter.adapters[path.first] as PreferencesSectionAdapter
        adapter.topics[path.second] = topic
        adapter.notifyItemChanged(path.second + 1)
    }

    fun setOnScrollPreferencesListener(listener: ((offsetInDp: Int) -> Unit)?) {
        onScrollPreferences = listener
    }

}

/**
 * Extensions
 */

fun CourierPreferences.scrollToTop() {
    recyclerView.smoothScrollToPosition(0)
}