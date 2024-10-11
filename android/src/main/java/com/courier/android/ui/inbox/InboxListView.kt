package com.courier.android.ui.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.Courier.Companion.coroutineScope
import com.courier.android.R
import com.courier.android.models.CourierException
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.models.InboxMessageSet
import com.courier.android.models.markAsClicked
import com.courier.android.models.markAsOpened
import com.courier.android.modules.clientKey
import com.courier.android.modules.fetchNextInboxPage
import com.courier.android.modules.refreshInbox
import com.courier.android.modules.userId
import com.courier.android.ui.infoview.CourierInfoView
import com.courier.android.utils.error
import com.courier.android.utils.forceReactNativeLayoutFix
import com.courier.android.utils.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

internal class InboxListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val feed: InboxMessageFeed = InboxMessageFeed.FEED
) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class State(var title: String? = null) {
        LOADING, ERROR, CONTENT, EMPTY
    }

    private var state = State.LOADING
        set(value) {
            field = value
            when (field) {
                State.LOADING -> {
                    recyclerView.isVisible = false
                    infoView.isVisible = false
                    loadingIndicator.isVisible = true
                }
                State.ERROR -> {
                    recyclerView.isVisible = false
                    infoView.isVisible = true
                    infoView.setTitle(field.title)
                    infoView.showButton(true)
                    loadingIndicator.isVisible = false
                }
                State.CONTENT -> {
                    recyclerView.isVisible = true
                    infoView.isVisible = false
                    infoView.setTitle(null)
                    loadingIndicator.isVisible = false
                }
                State.EMPTY -> {
                    recyclerView.isVisible = false
                    infoView.isVisible = true
                    infoView.setTitle(field.title)
                    infoView.showButton(false)
                    loadingIndicator.isVisible = false
                }
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

    }

    val recyclerView: RecyclerView by lazy { findViewById(R.id.recyclerView) }
    private val refreshLayout: SwipeRefreshLayout by lazy { findViewById(R.id.refreshLayout) }
    private val infoView: CourierInfoView by lazy { findViewById(R.id.infoView) }
    private val loadingIndicator: ProgressBar by lazy { findViewById(R.id.loadingIndicator) }

    private val layoutManager get() = recyclerView.layoutManager as? LinearLayoutManager

    private var onClickInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onClickInboxActionForMessageAtIndex: ((InboxAction, InboxMessage, Int) -> Unit)? = null
    private var onScrollInbox: ((Int) -> Unit)? = null

    private val messagesAdapter = MessagesAdapter(
        theme = theme,
        messages = mutableListOf(),
        onMessageClick = { message, index ->
            message.markAsClicked()
            onClickInboxMessageAtIndex?.invoke(message, index)
        },
        onActionClick = { action, message, index ->
            action.markAsClicked(message.messageId)
            onClickInboxActionForMessageAtIndex?.invoke(action, message, index)
        }
    )

    private val loadingAdapter = LoadingAdapter(
        theme = theme,
        onShown = {
            Courier.shared.fetchNextInboxPage(
                feed = feed,
                onSuccess = null,
                onFailure = null
            )
        }
    )

    private val adapter = ConcatAdapter(messagesAdapter)

    init {
        View.inflate(context, R.layout.courier_inbox_list_view, this)
        setup()
    }

    internal fun setLoading(isRefresh: Boolean) {

        // Show the refresh indicator
        refreshLayout.isRefreshing = isRefresh
        if (isRefresh) {
            return
        }

        // Show full loading
        state = State.LOADING
        refreshAdapters()
        recyclerView.forceReactNativeLayoutFix()

    }

    internal fun setError(e: Throwable) {
        state = State.ERROR.apply { title = e.message }
        Courier.shared.client?.error(e.message)
        refreshAdapters()
        recyclerView.forceReactNativeLayoutFix()
    }

    @SuppressLint("NotifyDataSetChanged")
    internal fun setMessageSet(set: InboxMessageSet) {

        refreshLayout.isRefreshing = false
        loadingAdapter.canPage = set.canPaginate

        refreshAdapters(
            showMessages = set.messages.isNotEmpty(),
            showLoading = set.canPaginate
        )

        messagesAdapter.messages = set.messages

        state = if (messagesAdapter.messages.isEmpty()) State.EMPTY.apply { title = "No messages found" } else State.CONTENT
        messagesAdapter.notifyDataSetChanged()
        openVisibleMessages()

        recyclerView.forceReactNativeLayoutFix()

    }

    internal fun addPage(set: InboxMessageSet) {

        refreshLayout.isRefreshing = false
        loadingAdapter.canPage = set.canPaginate

        refreshAdapters(
            showMessages = set.messages.isNotEmpty(),
            showLoading = set.canPaginate
        )

        // Get the current size of the existing messages
        val currentMessageCount = messagesAdapter.messages.size

        // Add new messages to the end of the list
        val newMessages = set.messages
        if (newMessages.isNotEmpty()) {
            messagesAdapter.messages.addAll(newMessages)
            state = if (messagesAdapter.messages.isEmpty()) State.EMPTY.apply { title = "No messages found" } else State.CONTENT
            messagesAdapter.notifyItemRangeInserted(currentMessageCount, newMessages.size)
        }

        openVisibleMessages()
        recyclerView.forceReactNativeLayoutFix()

    }

    internal fun addMessage(index: Int, message: InboxMessage) {

        refreshLayout.isRefreshing = false
        loadingAdapter.canPage = loadingAdapter.canPage

        refreshAdapters(
            showMessages = true,
            showLoading = loadingAdapter.canPage
        )

        state = State.CONTENT

        messagesAdapter.messages.add(index, message)
        messagesAdapter.notifyItemInserted(index)
        recyclerView.restoreScrollPosition()

        openVisibleMessages()
        recyclerView.forceReactNativeLayoutFix()

    }

    private fun setup() = coroutineScope.launch(Dispatchers.Main) {

        state = State.LOADING

        // Create the list
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
        refreshLayout.setOnRefreshListener {
            refresh()
        }

    }

    private fun refresh() = coroutineScope.launch(Dispatchers.Main) {
        theme.getBrandIfNeeded()
        Courier.shared.refreshInbox()
        refreshLayout.isRefreshing = false
    }

    private fun RecyclerView.restoreScrollPosition() {
        layoutManager?.apply {
            onRestoreInstanceState(onSaveInstanceState())
        }
    }

    // Opens all the current messages
    // Performs this on an async thread
    // Fails silently
    private fun openVisibleMessages() = coroutineScope.launch(Dispatchers.IO) {

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

                    if (message.isOpened) {
                        return@map null
                    }

                    // Mark the message as opened
                    return@map async {
                        message.setOpened()
                        message.markAsOpened()
                    }

                }

                // Perform all the changes together
                messagesToOpen.filterNotNull().awaitAll()

            } catch (e: CourierException) {

                Courier.shared.client?.error(e.message)

            }

        }

    }

//    @SuppressLint("NotifyDataSetChanged")
//    private fun refreshMessages(newMessages: List<InboxMessage>) {
//
////        val existingMessages = messagesAdapter.messages
//
//        // Set the new messages
//        messagesAdapter.messages = newMessages
//
//        // Check if we need to insert
////        val didInsert = newMessages.size - existingMessages.size == 1
//
////        // Handle insert
////        if (newMessages.firstOrNull()?.messageId != existingMessages.firstOrNull()?.messageId && didInsert) {
////            messagesAdapter.notifyItemInserted(0)
////            recyclerView.restoreScrollPosition()
////            return
////        }
//
////        // Handle pagination
////        if (newMessages.size > existingMessages.size) {
////            val firstIndex = existingMessages.size
////            val itemCount = newMessages.size - existingMessages.size
////            messagesAdapter.notifyItemRangeInserted(firstIndex, itemCount)
////            return
////        }
//
////        // Manually sync all view holders
////        // This ensures the click animation is nice and clean
////        if (newMessages.size == existingMessages.size) {
////            newMessages.forEachIndexed { index, message ->
////                val viewHolder = recyclerView.findViewHolderForLayoutPosition(index) as? MessageItemViewHolder
////                viewHolder?.setMessage(
////                    theme = theme,
////                    message = message
////                )
////            }
////            return
////        }
//
//        // Reload all other data
//        // Forces a hard reload
//        messagesAdapter.notifyDataSetChanged()
//
//        // Read new messages if possible
//        openVisibleMessages()
//
//    }

    private fun refreshAdapters(showMessages: Boolean = false, showLoading: Boolean = false) {
        if (showMessages) adapter.addAdapter(0, messagesAdapter) else adapter.removeAdapter(messagesAdapter)
        if (showLoading) adapter.addAdapter(loadingAdapter) else adapter.removeAdapter(loadingAdapter)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Reloads the inbox
        recyclerView.forceReactNativeLayoutFix()

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