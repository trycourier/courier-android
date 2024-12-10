package com.courier.android.ui.bar

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.courier.android.R
import com.courier.android.models.CourierBrand
import com.courier.android.utils.launchCourierWebsite

class CourierBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var courierBarButton: ImageView

    init {
        View.inflate(context, R.layout.courier_bar, this)
        setup()
    }

    private fun setup() {
        courierBarButton = findViewById(R.id.courierBarButton)
        courierBarButton.setOnClickListener { openDialog() }
        courierBarButton.setColorFilter(
            ContextCompat.getColor(context, R.color.footer_image_tint),
            PorterDuff.Mode.SRC_IN
        )
    }

    private fun openDialog() {

        AlertDialog.Builder(context).apply {

            setTitle("Learn more about Courier?")

            setNegativeButton("Cancel") { _, _ ->
                // Empty
            }

            setPositiveButton("Learn More") { _, _ ->
                context.launchCourierWebsite()
            }

            show()

        }

    }

    fun setBrand(brand: CourierBrand?) {
        brand?.let {
            isVisible = it.settings?.inapp?.showCourierFooter ?: true
        }
    }

}