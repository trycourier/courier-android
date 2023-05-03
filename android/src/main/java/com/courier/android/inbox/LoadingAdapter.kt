package com.courier.android.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.modules.fetchNextPageOfMessages

internal class LoadingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

internal class LoadingAdapter : RecyclerView.Adapter<LoadingItemViewHolder>() {

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