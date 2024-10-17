package com.courier.android.ui.inbox

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.courier.android.ui.CourierStyles
import com.courier.android.utils.setCourierFont

class BadgeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        // Set default text properties
        setTextColor(ContextCompat.getColor(context, android.R.color.white))
        setPadding(12, 4, 12, 4)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        // Set minimum width to 20dp
        val minWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
        ).toInt()
        minWidth = minWidthPx
    }

    private fun getPrimaryColor(): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }

    fun setStyle(style: CourierStyles.Inbox.TabIndicatorStyle) {
        background = getRoundedBackground(style.color ?: getPrimaryColor())
        setCourierFont(style.font)
        invalidate()
    }

    // Helper function to create a GradientDrawable with rounded corners
    private fun getRoundedBackground(color: Int): GradientDrawable {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 1000f
        }
        return drawable
    }
}
