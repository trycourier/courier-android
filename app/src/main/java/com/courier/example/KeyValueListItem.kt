package com.courier.example

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout

class KeyValueListItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val keyText: TextView
    private val valueText: TextView
    private val accessoryIcon: ImageView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_key_value_list_item, this, true)
        keyText = findViewById(R.id.keyText)
        valueText = findViewById(R.id.valueText)
        accessoryIcon = findViewById(R.id.accessoryIcon)

        val density = resources.displayMetrics.density
        val hPadding = (16 * density).toInt()
        val vPadding = (12 * density).toInt()
        setPadding(hPadding, vPadding, hPadding, vPadding)

        isClickable = true
        isFocusable = true
        val outValue = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
    }

    fun setKeyValue(key: String, value: String) {
        keyText.text = key
        valueText.text = value
    }

    fun setAccessoryIcon(@DrawableRes iconRes: Int?) {
        val gap = (12 * resources.displayMetrics.density).toInt()
        if (iconRes != null) {
            accessoryIcon.setImageResource(iconRes)
            accessoryIcon.visibility = View.VISIBLE
            (valueText.layoutParams as ConstraintLayout.LayoutParams).marginEnd = gap
        } else {
            accessoryIcon.visibility = View.GONE
            (valueText.layoutParams as ConstraintLayout.LayoutParams).marginEnd = 0
        }
    }
}
