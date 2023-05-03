package com.courier.android.inbox

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
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

class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

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

        }

    lateinit var recyclerView: RecyclerView
        private set

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

    private val loadingAdapter = LoadingAdapter()

    private val adapter = ConcatAdapter(messagesAdapter)

    init {
        View.inflate(context, R.layout.courier_inbox, this)
        refreshTheme()
        buildList()
    }

    private fun refreshTheme() {
        theme = if (context.isDarkMode) darkTheme else lightTheme
    }

    private fun buildList() {

        // Create the list
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        // Handle pull to refresh
        val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {

            Courier.shared.refreshInbox {
                refreshLayout.isRefreshing = false
            }

        }

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {
                refreshAdapters(
                    showLoading = true
                )
            },
            onError = { e ->
                print(e)
                refreshAdapters()
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->
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