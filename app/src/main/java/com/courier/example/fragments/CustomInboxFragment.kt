package com.courier.example.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.android.modules.fetchNextPageOfMessages
import com.courier.android.modules.inboxMessages
import com.courier.example.R
import com.courier.example.toJson


class CustomInboxFragment: Fragment(R.layout.fragment_custom_inbox) {

    private lateinit var inboxListener: CourierInboxListener
    private lateinit var recyclerView: RecyclerView

    private val messagesAdapter = MessagesAdapter()
    private val loadingAdapter = LoadingAdapter()
    private val adapter = ConcatAdapter(messagesAdapter)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Create the list
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        // Setup the listener
        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {
                adapter.addAdapter(loadingAdapter)
            },
            onError = { e ->
                print(e)
                adapter.removeAdapter(loadingAdapter)
                messagesAdapter.notifyDataSetChanged()
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->
                if (canPaginate) adapter.addAdapter(loadingAdapter) else adapter.removeAdapter(loadingAdapter)
                messagesAdapter.notifyDataSetChanged()
            }
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        inboxListener.remove()
    }

}

/**
 * List Item
 */

class MessageItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val textView: TextView

    init {
        textView = itemView.findViewById(R.id.textView)
    }

}

class MessagesAdapter : RecyclerView.Adapter<MessageItemViewHolder>() {

    private val messages get() = Courier.shared.inboxMessages.orEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return MessageItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageItemViewHolder, position: Int) {
        val item = messages[position]
        holder.textView.text = item.toJson()
        holder.textView.setOnClickListener {
            Courier.log("Item Clicked")
            Courier.log(item.toJson() ?: "Empty")
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

}

/**
 * Loading Item
 */

class LoadingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
class LoadingAdapter : RecyclerView.Adapter<LoadingItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.loading_item, parent, false)
        return LoadingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoadingItemViewHolder, position: Int) {
        Courier.shared.fetchNextPageOfMessages(onSuccess = { messages ->
            print(messages)
        })
    }

    override fun getItemCount(): Int {
        return 1
    }

}