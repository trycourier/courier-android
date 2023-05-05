package com.courier.android.inbox

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.courier.android.R
import com.courier.android.dpToPx
import com.courier.android.resIdToColorList
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

    fun setTheme(theme: CourierInboxTheme) {

        val buttonStyles = theme.buttonStyles

        // Background Color
        buttonStyles.backgroundColor?.let {
            button.backgroundTintList = it.resIdToColorList(context)
        }

        // Corner Radius
        buttonStyles.cornerRadiusInDp?.let {
            cornerRadius = it
        }

        // Typeface
        buttonStyles.font?.typeface?.let {
            button.typeface = ResourcesCompat.getFont(context, it)
        }

        // Color
        buttonStyles.font?.color?.let {
            button.setTextColor(ContextCompat.getColor(context, it))
        }

        // Text Size
        buttonStyles.font?.sizeInSp?.let {
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.toFloat())
        }

    }

    override fun getBaseline(): Int {
        return measuredHeight
    }

}