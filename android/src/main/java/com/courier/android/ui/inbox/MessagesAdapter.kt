package com.courier.android.ui.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.R
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.ui.CourierActionButton
import com.courier.android.ui.CourierStyles
import com.courier.android.utils.isDarkModeOn
import com.courier.android.utils.setCourierFont
import com.google.android.flexbox.FlexboxLayout

internal class MessageItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val container: ConstraintLayout
    private val titleTextView: TextView
    private val timeTextView: TextView
    private val subtitleTextView: TextView
    private val indicator: View
    private val dot: CourierCircleView
    private val dotContainer: FrameLayout
    private val buttonContainer: FlexboxLayout

    private var message: InboxMessage? = null

    private var onActionClick: ((InboxAction, InboxMessage) -> Unit)? = null
    private var onMessageClick: ((InboxMessage) -> Unit)? = null

    init {
        container = itemView.findViewById(R.id.container)
        titleTextView = itemView.findViewById(R.id.titleTextView)
        timeTextView = itemView.findViewById(R.id.timeTextView)
        subtitleTextView = itemView.findViewById(R.id.subtitleTextView)
        indicator = itemView.findViewById(R.id.indicator)
        dot = itemView.findViewById(R.id.dot)
        dotContainer = itemView.findViewById(R.id.dotContainer)
        buttonContainer = itemView.findViewById(R.id.buttonContainer)
    }

    private fun setIndicator(theme: CourierInboxTheme, message: InboxMessage) {

        when (theme.unreadIndicatorStyle.indicator) {
            CourierStyles.Inbox.UnreadIndicator.DOT -> {
                indicator.isVisible = false
                dot.isInvisible = message.isRead
            }
            CourierStyles.Inbox.UnreadIndicator.LINE -> {
                indicator.isVisible = !message.isRead
                dot.isVisible = false
            }
        }

    }

    fun setMessage(theme: CourierInboxTheme, message: InboxMessage) {

        this.message = message

        titleTextView.text = message.title
        timeTextView.text = message.time
        subtitleTextView.text = message.subtitle

        // Indicator
        setIndicator(theme, message)

        buttonContainer.isVisible = !message.actions.isNullOrEmpty()
        buttonContainer.removeAllViews()

        // Container
        container.setOnClickListener {
            onMessageClick?.invoke(message)
        }

        // Add the button actions
        message.actions?.forEach { action ->

            // Create the button for the action
            CourierActionButton(itemView.context).apply {
                val fallbackColor = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                this.setTheme(theme = theme, isRead = message.isRead, fallbackColor = if (message.isRead) fallbackColor else null)
                this.text = action.content
                this.onClick = {
                    onActionClick?.invoke(action, message)
                }
                buttonContainer.addView(this)
            }

        }

        // Theming
        theme.getUnreadColor()?.let {
            indicator.setBackgroundColor(it)
            dot.setCircleColor(it)
        }

        if (message.isRead) {

            val fallbackColor = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)

            titleTextView.setCourierFont(font = theme.titleStyle.read, fallbackColor = fallbackColor)
            subtitleTextView.setCourierFont(font = theme.bodyStyle.read, fallbackColor = fallbackColor)
            timeTextView.setCourierFont(font = theme.timeStyle.read, fallbackColor = fallbackColor)

        } else {

            val res = if (isDarkModeOn(itemView.context)) android.R.color.white else android.R.color.black
            val fallbackColor = ContextCompat.getColor(itemView.context, res)

            titleTextView.setCourierFont(font = theme.titleStyle.unread, fallbackColor = fallbackColor)
            subtitleTextView.setCourierFont(font = theme.bodyStyle.unread, fallbackColor = fallbackColor)
            timeTextView.setCourierFont(font = theme.timeStyle.unread, fallbackColor = fallbackColor)

        }

    }

    fun setInteraction(onActionClick: (InboxAction, InboxMessage) -> Unit, onMessageClick: (InboxMessage) -> Unit) {
        this.onActionClick = onActionClick
        this.onMessageClick = onMessageClick
    }

}

internal class MessagesAdapter(
    internal var theme: CourierInboxTheme,
    internal var messages: List<InboxMessage>,
    private val onMessageClick: (InboxMessage, Int) -> Unit,
    private val onActionClick: (InboxAction, InboxMessage, Int) -> Unit
) : RecyclerView.Adapter<MessageItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_inbox_list_item, parent, false)
        return MessageItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageItemViewHolder, position: Int) {

        holder.setMessage(
            theme = theme,
            message = messages[position]
        )

        holder.setInteraction(
            onActionClick = { action, msg ->
                onActionClick(action, msg, position)
            },
            onMessageClick = { msg ->
                onMessageClick(msg, position)
            }
        )

    }

    override fun getItemCount(): Int = messages.size

}