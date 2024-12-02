package com.courier.android.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.courier.android.R
import com.courier.android.ui.inbox.CourierInboxTheme
import com.courier.android.utils.dpToPx
import com.google.android.material.button.MaterialButton

internal class CourierActionButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

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
        clipChildren = false
        clipToPadding = false
        button = findViewById(R.id.button)
        button.setOnClickListener { onClick?.invoke() }
        cornerRadius = DEFAULT_CORNER_RADIUS
        removeShadow()
    }

    private fun removeShadow() {
        button.elevation = 0F
        button.stateListAnimator = null
    }

    fun setStyle(style: CourierStyles.Button, @ColorInt fallbackColor: Int? = null) {

        // Background Color
        (style.backgroundColor ?: fallbackColor)?.let {
            button.backgroundTintList = ColorStateList.valueOf(it)
        }

        // Set the styles
        style.cornerRadiusInDp?.let {
            cornerRadius = it
        }

        style.font?.typeface?.let {
            button.typeface = it
        }

        style.font?.color?.let {
            button.setTextColor(it)
        }

        style.font?.sizeInSp?.let {
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
        }

    }

    fun setTheme(theme: CourierInboxTheme, isRead: Boolean, @ColorInt fallbackColor: Int? = null) {

        // Background Color
        (theme.getButtonColor(isRead) ?: fallbackColor)?.let {
            button.backgroundTintList = ColorStateList.valueOf(it)
        }

        // Set the styles
        (if (isRead) theme.buttonStyle.read else theme.buttonStyle.unread).apply {
            this.cornerRadiusInDp?.let {
                cornerRadius = it
            }
            this.font?.typeface?.let {
                button.typeface = it
            }
            this.font?.color?.let {
                button.setTextColor(it)
            }
            this.font?.sizeInSp?.let {
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
            }
        }

    }

    override fun getBaseline(): Int {
        return measuredHeight
    }

}