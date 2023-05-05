package com.courier.android.inbox

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.modules.fetchNextPageOfMessages
import com.courier.android.resIdToColorList
import com.google.android.flexbox.FlexboxLayout

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
                progressBar.indeterminateTintList = it.resIdToColorList(itemView.context)
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