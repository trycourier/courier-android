package com.courier.android.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.inbox.*
import com.courier.android.models.*
import com.courier.android.modules.*
import com.courier.android.utils.isDarkMode
import com.courier.android.utils.pxToDp
import com.courier.android.utils.setCourierFont

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

    var lightTheme: CourierInboxTheme = CourierInboxTheme.DEFAULT_LIGHT
        set(value) {
            if (field != value) {
                field = value
                refreshTheme()
            }
        }

    var darkTheme: CourierInboxTheme = CourierInboxTheme.DEFAULT_DARK
        set(value) {
            if (field != value) {
                field = value
                refreshTheme()
            }
        }

    private var theme: CourierInboxTheme = CourierInboxTheme.DEFAULT_LIGHT
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

    private fun reloadViews() {

        // Loading indicator
        theme.getLoadingColor()?.let {
            refreshLayout.setColorSchemeColors(it)
        }

        // Divider line
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }

        theme.dividerItemDecoration?.let {
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
        }

    }

    lateinit var recyclerView: RecyclerView
        private set

    private val layoutManager get() = recyclerView.layoutManager as? LinearLayoutManager

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var infoView: LinearLayoutCompat
    private lateinit var detailTextView: TextView
    private lateinit var retryButton: CourierInboxButtonView
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
                // TODO
//                launchCourierWebsite()
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
    private fun refresh() {

        Courier.shared.getUserPreferences(
            onSuccess = { preferences ->

                val sections = mutableListOf<PreferencesSectionAdapter>()

                preferences.items.forEach { topic ->

                    val sectionId = topic.sectionId

                    // Find the section index
                    val sectionIndex = sections.indexOfFirst { it.section.sectionId == sectionId }

                    if (sectionIndex == -1) {

                        val i = sections.size

                        val newAdapter = PreferencesSectionAdapter(
                            theme = theme,
                            section = topic,
                            topics = mutableListOf(topic),
                            onTopicClick = { preferenceTopic, topicIndex ->
                                presentSheetForTopic(preferenceTopic, Pair(i, topicIndex))
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

            },
            onFailure = { e ->

                state = State.ERROR.apply { title = e.message }
                refreshLayout.isRefreshing = false

            }
        )

    }

    private fun presentSheetForTopic(topic: CourierPreferenceTopic, path: Pair<Int, Int>) {

        val sheet = PreferenceTopicBottomSheet(
            topic = topic,
            onDismiss = { newTopic ->
                updateTopic(topic, newTopic, path)
            }
        )

        sheet.show(context)

    }

    private fun updateTopic(originalTopic: CourierPreferenceTopic, newTopic: CourierPreferenceTopic, path: Pair<Int, Int>) {

        // Set the new topic
        setTopicAtPath(topic = newTopic, path = path)

        // Perform the change in the background
        // If fail, reset to original
        Courier.shared.putUserPreferenceTopic(
            topicId = newTopic.topicId,
            status = newTopic.status,
            hasCustomRouting = newTopic.hasCustomRouting,
            customRouting = newTopic.customRouting,
            onSuccess = {
                Courier.log("Preference Updated")
            },
            onFailure = { e ->
                Courier.error(e.message)
                setTopicAtPath(topic = originalTopic, path = path)
            }
        )

    }

    private fun setTopicAtPath(topic: CourierPreferenceTopic, path: Pair<Int, Int>) {
        val adapter = preferencesAdapter.adapters[path.first] as PreferencesSectionAdapter
        adapter.topics[path.second] = topic
        adapter.notifyItemChanged(path.second + 1)
    }

    private fun RecyclerView.restoreScrollPosition() {
        layoutManager?.apply {
            onRestoreInstanceState(onSaveInstanceState())
        }
    }

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Reloads the inbox
        recyclerView.forceReactNativeLayoutFix()

    }

}

/**
 * Extensions
 */

fun CourierPreferences.scrollToTop() {
    recyclerView.smoothScrollToPosition(0)
}