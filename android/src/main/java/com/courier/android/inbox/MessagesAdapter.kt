package com.courier.android.inbox

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.modules.inboxMessages
import com.courier.android.setCourierFont
import com.google.android.flexbox.FlexboxLayout

internal class MessageItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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

    fun setTheme(theme: CourierInboxTheme) {

        // Indicator Color
        theme.getUnreadColor()?.let {
            indicator.setBackgroundResource(it)
        }

        titleTextView.setCourierFont(theme.titleFont)
        subtitleTextView.setCourierFont(theme.bodyFont)
        timeTextView.setCourierFont(theme.timeFont)

    }

}

internal class MessagesAdapter(internal var theme: CourierInboxTheme, private val onMessageClick: (InboxMessage, Int) -> Unit, private val onActionClick: (InboxAction, InboxMessage, Int) -> Unit) : RecyclerView.Adapter<MessageItemViewHolder>() {

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

            // Indicator
            indicator.isVisible = !isRead

            buttonContainer.isVisible = !inboxMessage.actions.isNullOrEmpty()
            buttonContainer.removeAllViews()

            // Add the button actions
            inboxMessage.actions?.forEach { action ->

                // Create the button for the action
                CourierInboxButton(itemView.context).apply {
                    setTheme(theme)
                    text = action.content
                    onClick = { onActionClick(action, inboxMessage, position) }
                    buttonContainer.addView(this)
                }

            }

            // Handle item click
            container.setOnClickListener {
                onMessageClick(inboxMessage, position)
            }

            setTheme(theme)

        }

    }

    override fun getItemCount(): Int = messages.size

}