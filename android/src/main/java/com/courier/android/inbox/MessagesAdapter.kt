package com.courier.android.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.R
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.setCourierFont
import com.google.android.flexbox.FlexboxLayout

internal class MessageItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val container: ConstraintLayout
    val titleTextView: TextView
    val timeTextView: TextView
    val subtitleTextView: TextView
    val indicator: View
    val buttonContainer: FlexboxLayout

    private var message: InboxMessage? = null

    init {
        container = itemView.findViewById(R.id.container)
        titleTextView = itemView.findViewById(R.id.titleTextView)
        timeTextView = itemView.findViewById(R.id.timeTextView)
        subtitleTextView = itemView.findViewById(R.id.subtitleTextView)
        indicator = itemView.findViewById(R.id.indicator)
        buttonContainer = itemView.findViewById(R.id.buttonContainer)
    }

    fun setMessage(theme: CourierInboxTheme, message: InboxMessage) {

        this.message = message

        titleTextView.text = message.title
        timeTextView.text = message.time
        subtitleTextView.text = message.subtitle

        // Indicator
        indicator.isVisible = !message.isRead

        buttonContainer.isVisible = !message.actions.isNullOrEmpty()
        buttonContainer.removeAllViews()

        // Add the button actions
        message.actions?.forEach { action ->

            // Create the button for the action
            CourierInboxButton(itemView.context).apply {
                setTheme(theme)
                text = action.content
                buttonContainer.addView(this)
            }

        }

        // Theming
        theme.getUnreadColor()?.let {
            indicator.setBackgroundResource(it)
        }

        titleTextView.setCourierFont(theme.titleFont)
        subtitleTextView.setCourierFont(theme.bodyFont)
        timeTextView.setCourierFont(theme.timeFont)

    }

    fun setInteraction(onActionClick: (InboxAction) -> Unit, onMessageClick: () -> Unit) {

        buttonContainer.children.forEachIndexed { index, view ->
            view.setOnClickListener {
                message?.actions?.get(index)?.let { action ->
                    onActionClick(action)
                }
            }
        }

        container.setOnClickListener {
            onMessageClick()
        }

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

        val message = messages[position]

        holder.setMessage(
            theme = theme,
            message = message
        )

        holder.setInteraction(
            onActionClick = { action ->
                onActionClick(action, message, position)
            },
            onMessageClick = {
                onMessageClick(message, position)
            }
        )

    }

    override fun getItemCount(): Int = messages.size

}