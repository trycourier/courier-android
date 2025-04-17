package com.courier.android.ui.inbox

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.courier.android.Courier
import com.courier.android.utils.setCourierFont
import com.courier.android.utils.toHex

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

        // Center text horizontally and vertically
        textAlignment = TEXT_ALIGNMENT_CENTER
        gravity = Gravity.CENTER

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

    fun setTheme(theme: CourierInboxTheme, isSelected: Boolean) {
        background = getRoundedBackground(theme.getBadgeColor(isSelected) ?: getPrimaryColor())
        val font = if (isSelected) theme.tabStyle.selected.indicator.font else theme.tabStyle.unselected.indicator.font
        setCourierFont(font)
        invalidate()
    }

    // Helper function to create a GradientDrawable with rounded corners
    private fun getRoundedBackground(color: Int): GradientDrawable {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 1000f
        }
        if (Courier.shared.isUITestsActive) {
            tag = "background, color: ${color.toHex()}"
        }
        return drawable
    }
}
