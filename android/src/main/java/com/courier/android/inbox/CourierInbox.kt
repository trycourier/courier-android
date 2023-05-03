package com.courier.android.inbox

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.models.*
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.fetchNextPageOfMessages
import com.courier.android.modules.inboxMessages
import com.courier.android.modules.refreshInbox
import com.google.android.flexbox.FlexboxLayout
import com.google.gson.GsonBuilder

class CourierInbox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    lateinit var recyclerView: RecyclerView
        private set

    private lateinit var inboxListener: CourierInboxListener
    private var onClickInboxMessageAtIndex: ((InboxMessage, Int) -> Unit)? = null
    private var onClickInboxActionForMessageAtIndex: ((InboxAction, InboxMessage, Int) -> Unit)? = null

//    var onScrollInbox: ((UIScrollView) -> Unit)? = null

    private val messagesAdapter = MessagesAdapter(
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
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {

        // Create the layout from xml
        View.inflate(context, R.layout.courier_inbox, this)

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

    /**
     * List Item
     */

    private class MessageItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val container: ConstraintLayout
        val titleTextView: TextView
        val timeTextView: TextView
        val subtitleTextView: TextView
        val indicator: View
        val buttonContainer: FlexboxLayout

        init {
            container = itemView.findViewById(R.id.container)
            titleTextView = itemView.findViewById(R.id.titleTextView)
            timeTextView = itemView.findViewById(R.id.timeTextView)
            subtitleTextView = itemView.findViewById(R.id.subtitleTextView)
            indicator = itemView.findViewById(R.id.indicator)
            buttonContainer = itemView.findViewById(R.id.buttonContainer)
        }

    }

    private class MessagesAdapter(private val onMessageClick: (InboxMessage, Int) -> Unit, private val onActionClick: (InboxAction, InboxMessage, Int) -> Unit) : RecyclerView.Adapter<MessageItemViewHolder>() {

        private val messages get() = Courier.shared.inboxMessages.orEmpty()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_inbox_list_item, parent, false)
            return MessageItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageItemViewHolder, position: Int) {

            val inboxMessage = messages[position]
            val isRead = inboxMessage.isRead

            holder.apply {

                titleTextView.text = inboxMessage.title
                timeTextView.text = inboxMessage.time
                subtitleTextView.text = inboxMessage.subtitle
                indicator.isVisible = !isRead
                buttonContainer.isVisible = !inboxMessage.actions.isNullOrEmpty()
                buttonContainer.removeAllViews()

                // Add the button actions
                inboxMessage.actions?.forEach { action ->

                    // Create the button for the action
                    CourierInboxButton(holder.itemView.context).apply {
                        text = action.content
                        buttonContainer.addView(this)
                        onClick = {
                            onActionClick(action, inboxMessage, position)
                        }
                    }

                }

                // Handle item click
                container.setOnClickListener {
                    onMessageClick(inboxMessage, position)
                }

            }

        }

        override fun getItemCount(): Int {
            return messages.size
        }

    }

    /**
     * Loading Item
     */

    private class LoadingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private class LoadingAdapter : RecyclerView.Adapter<LoadingItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_inbox_loading_item, parent, false)
            return LoadingItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: LoadingItemViewHolder, position: Int) {
            Courier.shared.fetchNextPageOfMessages(
                onSuccess = { newMessages ->
                    print(newMessages)
                },
                onFailure = { error ->
                    print(error)
                }
            )
        }

        override fun getItemCount(): Int {
            return 1
        }

    }

}

fun CourierInbox.scrollToTop() {
    recyclerView.smoothScrollToPosition(0)
}