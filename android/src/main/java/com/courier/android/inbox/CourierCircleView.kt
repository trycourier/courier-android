package com.courier.android.inbox

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.courier.android.R

internal class CourierCircleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint()
    private var circleColor: Int = Color.RED // Default color is red

    init {
        initAttributes(context, attrs)
        paint.style = Paint.Style.FILL
        updatePaintColor(context)
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CourierCircleView)
        circleColor = typedArray.getColor(
            R.styleable.CourierCircleView_circleColor,
            0 // Default value is 0, which means it will use the colorPrimary attribute
        )
        typedArray.recycle()
    }

    private fun updatePaintColor(context: Context) {
        if (circleColor == 0) {
            val typedValue = android.util.TypedValue()
            val theme = context.theme
            theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            circleColor = typedValue.data
        }
        paint.color = circleColor
        invalidate()
    }

    fun setCircleColor(@ColorInt color: Int) {
        circleColor = color
        updatePaintColor(context)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width // Get the width of the view
        val height = height // Get the height of the view
        val radius = if (width < height) width / 2f else height / 2f // Calculate the radius as half of the smaller dimension

        val centerX = width / 2f // Calculate the X coordinate of the circle's center
        val centerY = height / 2f // Calculate the Y coordinate of the circle's center

        // Adjust radius to ensure the circle fits within the view bounds
        val adjustedRadius = if (radius > centerX || radius > centerY) {
            // If the radius is larger than the center coordinates, adjust it to fit within the view
            minOf(centerX, centerY)
        } else {
            radius
        }

        // Draw the circle in the center of the view without clipping
        canvas.drawCircle(centerX, centerY, adjustedRadius, paint)

    }

}