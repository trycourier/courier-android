package com.courier.example

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class KeyValueListItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val keyText: TextView
    private val valueText: TextView

    init {
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_key_value_list_item, this, true)

        // Initialize views
        keyText = findViewById(R.id.keyText)
        valueText = findViewById(R.id.valueText)
    }

    fun setKeyValue(key: String, value: String) {
        keyText.text = key
        valueText.text = value
    }
}