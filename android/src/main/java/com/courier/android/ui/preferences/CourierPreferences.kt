package com.courier.android.ui.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.R
import com.courier.android.models.CourierAgent
import com.courier.android.models.CourierException
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.models.CourierPreferenceStatus
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.modules.getUserPreferences
import com.courier.android.modules.putUserPreferenceTopic
import com.courier.android.ui.CourierActionButton
import com.courier.android.utils.isDarkMode
import com.courier.android.utils.launchCourierWebsite
import com.courier.android.utils.pxToDp
import com.courier.android.utils.setCourierFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourierPreferences @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class State(var title: String? = null) {
        LOADING, ERROR, CONTENT, EMPTY
    }

    private var state = State.LOADING
        set(value) {
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
                    detailTextView.text = field.title
                    loadingIndicator.isVisible = false
                }
                State.CONTENT -> {
                    refreshLayout.isVisible = true
                    infoView.isVisible = false
                    detailTextView.text = null
                    loadingIndicator.isVisible = false
                }
                State.EMPTY -> {
                    refreshLayout.isVisible = false
                    infoView.isVisible = true
                    detailTextView.text = field.title
                    loadingIndicator.isVisible = false
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

    private fun reloadViews() {

        // Divider line
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }

        theme.topicDividerItemDecoration?.let {
            recyclerView.addItemDecoration(it)
        }

        // Empty / Error view
        detailTextView.setCourierFont(
            font = theme.infoViewStyle.font
        )

        // Button
        retryButton.apply {

            setStyle(
                style = theme.infoViewStyle.button
            )

            text = "Retry"
            onClick = {
                state = State.LOADING
                refresh()
            }

        }

        // Loading
        theme.getLoadingColor()?.let {
            loadingIndicator.indeterminateTintList = ColorStateList.valueOf(it)
            refreshLayout.setColorSchemeColors(it)
        }

        // Handle bar visibility
        val showBar = theme.brand?.settings?.inapp?.showCourierFooter ?: true
        courierBar.isVisible = showBar

    }

    lateinit var recyclerView: RecyclerView
        private set

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var infoView: LinearLayoutCompat
    private lateinit var detailTextView: TextView
    private lateinit var retryButton: CourierActionButton
    private lateinit var courierBar: RelativeLayout
    private lateinit var courierBarButton: ImageView
    private lateinit var loadingIndicator: ProgressBar

    private var onScrollInbox: ((Int) -> Unit)? = null

    private var preferencesAdapter = ConcatAdapter()

    init {
        View.inflate(context, R.layout.courier_preferences, this)
        setup()
        refreshTheme()
    }

    private fun refreshTheme() {
        theme = if (context.isDarkMode) darkTheme else lightTheme
    }

    private fun openDialog() {

        AlertDialog.Builder(context).apply {

            setTitle("Learn more about Courier?")

            setNegativeButton("Cancel") { _, _ ->
                // Empty
            }

            setPositiveButton("Learn More") { _, _ ->
                context.launchCourierWebsite()
            }

            show()

        }

    }

    private fun setup() {

        // Loading
        loadingIndicator = findViewById(R.id.loadingIndicator)

        // Courier Bar Button
        courierBarButton = findViewById(R.id.courierBarButton)
        courierBarButton.setOnClickListener { openDialog() }

        // Info View
        infoView = findViewById(R.id.infoView)
        detailTextView = findViewById(R.id.detailTextView)
        retryButton = findViewById(R.id.retryButton)

        // Courier Bar
        courierBar = findViewById(R.id.courierBar)

        // Create the list
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->

            // Get the current offset if needed
            onScrollInbox?.let {
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

        // Get the brand
        theme.getBrandIfNeeded()

        // Get the preferences
        try {

            val preferences = Courier.shared.getUserPreferences()

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
            preferencesAdapter.notifyDataSetChanged()

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
                Courier.shared.putUserPreferenceTopic(
                    topicId = originalTopic.topicId,
                    status = newStatus,
                    hasCustomRouting = newTopic.hasCustomRouting,
                    customRouting = newTopic.customRouting,
                    onSuccess = {
                        Courier.log("Topic updated: ${originalTopic.topicId}")
                    },
                    onFailure = { error ->
                        Courier.error(error.message)
                        onError?.invoke(error as CourierException)
                        setTopicAtPath(topic = originalTopic, path = path)
                    }
                )
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
                Courier.shared.putUserPreferenceTopic(
                    topicId = originalTopic.topicId,
                    status = newStatus,
                    hasCustomRouting = hasCustomRouting,
                    customRouting = customRouting,
                    onSuccess = {
                        Courier.log("Topic updated: ${originalTopic.topicId}")
                    },
                    onFailure = { error ->
                        Courier.error(error.message)
                        onError?.invoke(error as CourierException)
                        setTopicAtPath(topic = originalTopic, path = path)
                    }
                )
            }
        }
    }

    private fun setTopicAtPath(topic: CourierPreferenceTopic, path: Pair<Int, Int>) {
        val adapter = preferencesAdapter.adapters[path.first] as PreferencesSectionAdapter
        adapter.topics[path.second] = topic
        adapter.notifyItemChanged(path.second + 1)
    }

//    private fun RecyclerView.restoreScrollPosition() {
//        layoutManager?.apply {
//            onRestoreInstanceState(onSaveInstanceState())
//        }
//    }

    @SuppressLint("NotifyDataSetChanged")
    private fun RecyclerView.forceReactNativeLayoutFix() {

        if (Courier.USER_AGENT != CourierAgent.REACT_NATIVE_ANDROID) {
            return
        }

        try {

            // Forces the layout to refresh
            // This is a react native bug
            adapter?.notifyDataSetChanged()
            scrollBy(0, 0)

        } catch (e: Exception) {

            Courier.error(e.toString())

        }

    }

//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//
//        // Reloads the inbox
//        recyclerView.forceReactNativeLayoutFix()
//
//    }

}

/**
 * Extensions
 */

fun CourierPreferences.scrollToTop() {
    recyclerView.smoothScrollToPosition(0)
}