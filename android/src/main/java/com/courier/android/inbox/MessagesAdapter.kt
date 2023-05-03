package com.courier.android.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.models.InboxAction
import com.courier.android.models.InboxMessage
import com.courier.android.modules.inboxMessages
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

            theme.getUnreadColor()?.let {
                indicator.setBackgroundResource(it)
            }

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