package com.courier.android.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson

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

            // Reload the list
            preferencesAdapter.theme = theme
            preferencesAdapter.notifyDataSetChanged()

            loadingAdapter.theme = theme
            loadingAdapter.notifyDataSetChanged()

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

    private val preferencesAdapter = PreferencesAdapter(
        theme = theme,
        topics = emptyList(),
        onTopicClick = { topic, _ ->
            openSheet(topic = topic)
        }
    )

    private fun openSheet(topic: CourierPreferenceTopic) {

        // Inflate the bottom sheet layout
        val bottomSheetView: View = LayoutInflater.from(context).inflate(R.layout.preference_topic_sheet, null)

        // Find the TextView in the bottom sheet layout
        val textView: TextView = bottomSheetView.findViewById(R.id.textView)
        textView.text = Gson().toJson(topic).toString()

        // Create a BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(context)

        // Set the view to the BottomSheetDialog
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetDialog.setOnDismissListener {
            print("Close")
        }

        bottomSheetDialog.show()

    }

    private val loadingAdapter = LoadingAdapter(
        theme = theme
    )

    private val adapter = ConcatAdapter(preferencesAdapter)

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
        recyclerView.adapter = adapter
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
                preferencesAdapter.topics = preferences.items
                state = if (preferences.items.isEmpty()) State.EMPTY.apply { title = "No preferences found" } else State.CONTENT
                preferencesAdapter.notifyDataSetChanged()
                refreshLayout.isRefreshing = false
            },
            onFailure = { e ->
                state = State.ERROR.apply { title = e.message }
                refreshLayout.isRefreshing = false
            }
        )

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