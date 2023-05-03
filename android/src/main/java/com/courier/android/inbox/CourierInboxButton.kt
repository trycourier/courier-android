package com.courier.android.inbox

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.courier.android.R
import com.courier.android.dpToPx
import com.google.android.material.button.MaterialButton

internal class CourierInboxButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    var onClick: (() -> Unit)? = null

    companion object {
        const val DEFAULT_CORNER_RADIUS = 6
    }

    private var button: MaterialButton

    var text: String? = null
        set(value) {
            field = value
            button.text = field
        }

    var cornerRadius: Int = DEFAULT_CORNER_RADIUS.dpToPx
        set(value) {
            field = value
            button.cornerRadius = field
        }

    init {
        View.inflate(context, R.layout.courier_inbox_button, this)
        button = findViewById(R.id.button)
        button.setOnClickListener { onClick?.invoke() }
        cornerRadius = DEFAULT_CORNER_RADIUS.dpToPx
    }

}