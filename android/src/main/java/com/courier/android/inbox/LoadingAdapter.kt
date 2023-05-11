package com.courier.android.inbox

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.modules.fetchNextPageOfMessages

internal class LoadingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val progressBar: ProgressBar
    init {
        progressBar = itemView.findViewById(R.id.progressBar)
    }

}

internal class LoadingAdapter(internal var theme: CourierInboxTheme) : RecyclerView.Adapter<LoadingItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_inbox_loading_item, parent, false)
        return LoadingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoadingItemViewHolder, position: Int) {

        holder.apply {

            theme.getLoadingColor()?.let {
                progressBar.indeterminateTintList = ColorStateList.valueOf(it)
            }

        }

        Courier.shared.fetchNextPageOfMessages(
            onSuccess = { newMessages ->
                print(newMessages)
            },
            onFailure = { error ->
                print(error)
            }
        )

    }

    override fun getItemCount(): Int = 1

}