package com.courier.android.inbox

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.courier.android.R
import com.courier.android.utils.dpToPx
import com.google.android.material.button.MaterialButton

internal class CourierInboxButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

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

    var cornerRadius: Int = DEFAULT_CORNER_RADIUS
        private set(value) {
            field = value
            button.cornerRadius = field.dpToPx
        }

    init {
        View.inflate(context, R.layout.courier_inbox_button, this)
        button = findViewById(R.id.button)
        button.setOnClickListener { onClick?.invoke() }
        cornerRadius = DEFAULT_CORNER_RADIUS
    }

    fun setStyle(style: CourierInboxButton, @ColorInt fallbackColor: Int? = null) {

        // Background Color
        (style.backgroundColor ?: fallbackColor)?.let {
            button.backgroundTintList = ColorStateList.valueOf(it)
        }

        // Corner Radius
        style.cornerRadiusInDp?.let {
            cornerRadius = it
        }

        // Typeface
        style.font?.typeface?.let {
            button.typeface = it
        }

        // Color
        style.font?.color?.let {
            button.setTextColor(it)
        }

        // Text Size
        style.font?.sizeInSp?.let {
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
        }

    }

    override fun getBaseline(): Int {
        return measuredHeight
    }

}