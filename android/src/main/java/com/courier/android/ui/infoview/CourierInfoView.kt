package com.courier.android.ui.infoview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.courier.android.R
import com.courier.android.ui.CourierActionButton
import com.courier.android.ui.inbox.CourierInboxTheme
import com.courier.android.ui.preferences.CourierPreferencesTheme
import com.courier.android.utils.setCourierFont

class CourierInfoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var titleTextView: TextView
    private lateinit var retryButton: CourierActionButton

    internal var onRetry: (() -> Unit)? = null

    init {
        View.inflate(context, R.layout.courier_info_view, this)
        setup()
    }

    private fun setup() {
        titleTextView = findViewById(R.id.titleTextView)
        retryButton = findViewById(R.id.retryButton)
    }

    fun setTitle(title: String?) {
        titleTextView.text = title
    }

    fun setTheme(theme: CourierInboxTheme) {

        titleTextView.setCourierFont(
            font = theme.infoViewStyle.font
        )

        retryButton.apply {

            setStyle(
                style = theme.infoViewStyle.button
            )

            text = "Retry"
            onClick = onRetry

        }

    }

    fun setTheme(theme: CourierPreferencesTheme) {

        titleTextView.setCourierFont(
            font = theme.infoViewStyle.font
        )

        retryButton.apply {

            setStyle(
                style = theme.infoViewStyle.button
            )

            text = "Retry"
            onClick = onRetry

        }

    }

}