package com.courier.android.ui.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.R
import com.courier.android.models.CourierException
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.clickMessage
import com.courier.android.modules.clientKey
import com.courier.android.modules.refreshInbox
import com.courier.android.modules.userId
import com.courier.android.repositories.InboxRepository
import com.courier.android.ui.bar.CourierBar
import com.courier.android.ui.infoview.CourierInfoView
import com.courier.android.ui.preferences.inbox.LoadingAdapter
import com.courier.android.utils.forceReactNativeLayoutFix
import com.courier.android.utils.isDarkMode
import com.courier.android.utils.launchCourierWebsite
import com.courier.android.utils.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

open class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

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
            messagesAdapter.theme = theme
            messagesAdapter.notifyDataSetChanged()

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

        infoView.setTheme(theme)
        infoView.onRetry = {
            state = State.LOADING
            refresh()
        }

        theme.getLoadingColor()?.let {
            loadingIndicator.indeterminateTintList = ColorStateList.valueOf(it)
        }

        // Handle bar brand
        courierBar.setBrand(theme.brand)

    }

    lateinit var recyclerView: RecyclerView
        private set

    private val layoutManager get() = recyclerView.layoutManager as? LinearLayoutManager

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var infoView: CourierInfoView
    private lateinit var courierBar: CourierBar
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var inboxListener: CourierInboxListener

    private var onClickInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onClickInboxActionForMessageAtIndex: ((InboxAction, InboxMessage, Int) -> Unit)? = null
    private var onScrollInbox: ((Int) -> Unit)? = null

    private val inboxRepo by lazy { InboxRepository() }

    private val messagesAdapter = MessagesAdapter(
        theme = theme,
        messages = emptyList(),
        onMessageClick = { message, index ->

            // Handle message click
            Courier.shared.clickMessage(message.messageId, onFailure = null)

            // Call the callback
            onClickInboxMessageAtIndex?.invoke(message, index)

        },
        onActionClick = { action, message, index ->
            onClickInboxActionForMessageAtIndex?.invoke(action, message, index)
        }
    )

    private val loadingAdapter = LoadingAdapter(
        theme = theme
    )

    private val adapter = ConcatAdapter(messagesAdapter)

    init {
        View.inflate(context, R.layout.courier_inbox, this)
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

        // Info View
        infoView = findViewById(R.id.infoView)

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

            // Open the messages
            openVisibleMessages()

        }

        // Handle pull to refresh
        refreshLayout = findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            refresh()
        }

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {

                refreshBrand()

                state = State.LOADING

                refreshAdapters()

                recyclerView.forceReactNativeLayoutFix()

            },
            onError = { e ->

                refreshBrand()

                state = State.ERROR.apply { title = e.message }

                Courier.error(e.message)

                refreshAdapters()

                recyclerView.forceReactNativeLayoutFix()

            },
            onMessagesChanged = { messages, _, _, canPaginate ->

                refreshBrand()

                state = if (messages.isEmpty()) State.EMPTY.apply { title = "No messages found" } else State.CONTENT

                refreshAdapters(
                    showMessages = messages.isNotEmpty(),
                    showLoading = canPaginate
                )

                refreshMessages(
                    newMessages = messages.toList()
                )

                openVisibleMessages()

                recyclerView.forceReactNativeLayoutFix()

            }
        )

    }

    private fun refresh() {
        Courier.shared.refreshInbox {
            refreshLayout.isRefreshing = false
        }
    }

    private fun RecyclerView.restoreScrollPosition() {
        layoutManager?.apply {
            onRestoreInstanceState(onSaveInstanceState())
        }
    }

    // Opens all the current messages
    // Performs this on an async thread
    // Fails silently
    private fun openVisibleMessages() = coroutineScope.launch(context = Dispatchers.IO) {

        // Ensure we have a user
        if (Courier.shared.clientKey == null || Courier.shared.userId == null) {
            return@launch
        }

        // Get all the visible items
        layoutManager?.let { manager ->

            // Get the indexes
            val firstIndex = manager.findFirstCompletelyVisibleItemPosition()
            val lastIndex = manager.findLastCompletelyVisibleItemPosition()

            // Avoid index out of bounds
            if (firstIndex == -1 || lastIndex == -1) {
                return@let
            }

            try {

                // Find the messages
                val messagesToOpen = messagesAdapter.messages.subList(firstIndex, lastIndex).filter { !it.isOpened }.map { message ->

                    // Mark the message as open locally
                    message.setOpened()

                    // Bundle the request into an async function
                    return@map async {

                        // Open the message in Courier
                        inboxRepo.openMessage(
                            clientKey = Courier.shared.clientKey!!,
                            userId = Courier.shared.userId!!,
                            messageId = message.messageId,
                        )

                    }

                }

                // Perform all the changes together
                messagesToOpen.awaitAll()

            } catch (e: CourierException) {

                Courier.error(e.message)

            }

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshMessages(newMessages: List<InboxMessage>) {

        val existingMessages = messagesAdapter.messages

        // Set the new messages
        messagesAdapter.messages = newMessages

        // Check if we need to insert
        val didInsert = newMessages.size - existingMessages.size == 1

        // Handle insert
        if (newMessages.firstOrNull()?.messageId != existingMessages.firstOrNull()?.messageId && didInsert) {
            messagesAdapter.notifyItemInserted(0)
            recyclerView.restoreScrollPosition()
            return
        }

        // Handle pagination
        if (newMessages.size > existingMessages.size) {
            val firstIndex = existingMessages.size
            val itemCount = newMessages.size - existingMessages.size
            messagesAdapter.notifyItemRangeInserted(firstIndex, itemCount)
            return
        }

        // Manually sync all view holders
        // This ensure the click animation is nice and clean
        if (newMessages.size == existingMessages.size) {
            newMessages.forEachIndexed { index, message ->
                val viewHolder = recyclerView.findViewHolderForLayoutPosition(index) as? MessageItemViewHolder
                viewHolder?.setMessage(
                    theme = theme,
                    message = message
                )
            }
            return
        }

        // Reload all other data
        // Forces a hard reload
        messagesAdapter.notifyDataSetChanged()

        // Read new messages if possible
        openVisibleMessages()

    }

    private fun refreshAdapters(showMessages: Boolean = false, showLoading: Boolean = false) {
        if (showMessages) adapter.addAdapter(0, messagesAdapter) else adapter.removeAdapter(messagesAdapter)
        if (showLoading) adapter.addAdapter(loadingAdapter) else adapter.removeAdapter(loadingAdapter)
    }

    private fun refreshBrand() = coroutineScope.launch(Dispatchers.Main) {
        theme.getBrandIfNeeded()
        reloadViews()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Reloads the inbox
        recyclerView.forceReactNativeLayoutFix()

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        inboxListener.remove()
    }

    fun setOnClickMessageListener(listener: ((message: InboxMessage, index: Int) -> Unit)?) {
        onClickInboxMessageAtIndex = listener
    }

    fun setOnClickActionListener(listener: ((action: InboxAction, message: InboxMessage, index: Int) -> Unit)?) {
        onClickInboxActionForMessageAtIndex = listener
    }

    fun setOnScrollInboxListener(listener: ((offsetInDp: Int) -> Unit)?) {
        onScrollInbox = listener
    }


}

/**
 * Extensions
 */

fun CourierInbox.scrollToTop() {
    recyclerView.smoothScrollToPosition(0)
}