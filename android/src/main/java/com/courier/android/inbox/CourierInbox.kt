package com.courier.android.inbox

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.isDarkMode
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.refreshInbox
import com.courier.android.setCourierFont
import kotlin.coroutines.resume

class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class State(var title: String? = null) {
        LOADING, ERROR, CONTENT, EMPTY
    }

    private var state = State.LOADING
        set(value) {
            field = value
            when (field) {
                State.LOADING -> {
                    refreshLayout.isVisible = false
                    detailTextView.isVisible = true
                    detailTextView.text = field.title
                }
                State.ERROR -> {
                    refreshLayout.isVisible = false
                    detailTextView.isVisible = true
                    detailTextView.text = field.title
                }
                State.CONTENT -> {
                    refreshLayout.isVisible = true
                    detailTextView.isVisible = false
                    detailTextView.text = null
                }
                State.EMPTY -> {
                    refreshLayout.isVisible = false
                    detailTextView.isVisible = true
                    detailTextView.text = field.title
                }
            }
        }

    var lightTheme: CourierInboxTheme = CourierInboxTheme.DEFAULT_LIGHT
        set(value) {
            field = value
            refreshTheme()
        }

    var darkTheme: CourierInboxTheme = CourierInboxTheme.DEFAULT_DARK
        set(value) {
            field = value
            refreshTheme()
        }

    private var theme: CourierInboxTheme = CourierInboxTheme.DEFAULT_LIGHT
        set(value) {
            field = value

            // TODO: Cleanup
            messagesAdapter.theme = theme
            messagesAdapter.notifyDataSetChanged()

            loadingAdapter.theme = theme
            loadingAdapter.notifyDataSetChanged()

            // Loading indicator
            theme.getLoadingColor()?.let {
                val color = ContextCompat.getColor(context, it)
                refreshLayout.setColorSchemeColors(color)
            }

            // Divider line
            if (recyclerView.itemDecorationCount > 0) {
                recyclerView.removeItemDecorationAt(0)
            }

            theme.dividerItemDecoration?.let {
                recyclerView.addItemDecoration(it)
            }

            // Empty / Error view
            detailTextView.setCourierFont(theme.detailTitleFont)

        }

    lateinit var recyclerView: RecyclerView
        private set

    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var detailTextView: TextView
    private lateinit var courierBar: View
    private lateinit var courierBarButton: ImageView

    private lateinit var inboxListener: CourierInboxListener
    private var onClickInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onClickInboxActionForMessageAtIndex: ((InboxAction, InboxMessage, Int) -> Unit)? = null

    private val messagesAdapter = MessagesAdapter(
        theme = theme,
        onMessageClick = { message, index ->
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
                openCourier()
            }

            show()

        }

    }

    private fun openCourier() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.courier.com/"))
        context.startActivity(browserIntent)
    }

    private fun setup() {

        // Courier Bar Button
        courierBarButton = findViewById(R.id.courierBarButton)
        courierBarButton.setOnClickListener { openDialog() }

        // Detail TextView
        detailTextView = findViewById(R.id.detailTextView)

        // Create the list
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        // Handle pull to refresh
        refreshLayout = findViewById(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {

            Courier.shared.refreshInbox {
                refreshLayout.isRefreshing = false
            }

        }

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {
                state = State.LOADING.apply { title = "Loading" }
                refreshAdapters(
                    showLoading = true
                )
            },
            onError = { e ->
                Courier.error(e.message)
                state = State.ERROR.apply { title = e.message }
                refreshAdapters()
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->
                state = if (messages.isEmpty()) State.EMPTY.apply { title = "No messages found" } else State.CONTENT
                refreshAdapters(
                    showMessages = messages.isNotEmpty(),
                    showLoading = canPaginate
                )
            }
        )

    }

    private fun refreshAdapters(showMessages: Boolean = false, showLoading: Boolean = false) {
        if (showMessages) adapter.addAdapter(0, messagesAdapter) else adapter.removeAdapter(messagesAdapter)
        if (showLoading) adapter.addAdapter(loadingAdapter) else adapter.removeAdapter(loadingAdapter)
        messagesAdapter.notifyDataSetChanged()
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

}

/**
 * Extensions
 */

fun CourierInbox.scrollToTop() {
    recyclerView.smoothScrollToPosition(0)
}