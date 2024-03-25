package com.courier.android.preferences

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.R
import com.courier.android.utils.isDarkModeOn
import com.courier.android.utils.setCourierFont

internal data class CourierSheetItem(
    val title: String,
    var isOn: Boolean,
    val isDisabled: Boolean,
    val data: Any?
)

internal class SheetItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val container: LinearLayoutCompat
    private val textView: TextView
    private val switchView: SwitchCompat

    init {
        container = itemView.findViewById(R.id.container)
        textView = itemView.findViewById(R.id.textView)
        switchView = itemView.findViewById(R.id.switchView)
    }

    fun setItem(theme: CourierPreferencesTheme, item: CourierSheetItem, onItemChange: (CourierSheetItem) -> Unit) {

        textView.text = item.title
        switchView.isChecked = item.isOn
        switchView.isEnabled = !item.isDisabled

        val res = if (isDarkModeOn(itemView.context)) android.R.color.white else android.R.color.black
        val fallbackColor = ContextCompat.getColor(itemView.context, res)

        // Set the styles
        textView.setCourierFont(font = theme.sheetSettingStyles.font, fallbackColor = fallbackColor)
        theme.sheetSettingStyles.toggleThumbColor?.let {
            switchView.thumbTintList = ColorStateList.valueOf(it)
        }
        theme.sheetSettingStyles.toggleTrackColor?.let {
            switchView.trackTintList = ColorStateList.valueOf(it)
        }

        if (switchView.isEnabled) {

            container.setOnClickListener {

                switchView.isChecked = !switchView.isChecked

                // Create a new item and pass it to the callbacks
                val newItem = CourierSheetItem(
                    title = item.title,
                    isOn = switchView.isChecked,
                    isDisabled = !switchView.isEnabled,
                    data = item.data
                )

                onItemChange(newItem)

            }

        }

    }

}

internal class PreferencesSheetItemAdapter(
    internal val theme: CourierPreferencesTheme,
    private val items: List<CourierSheetItem>,
    private val onItemChange: (CourierSheetItem, Int) -> Unit
) : RecyclerView.Adapter<SheetItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SheetItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.courier_preferences_sheet_item, parent, false)
        return SheetItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: SheetItemViewHolder, position: Int) {
        holder.setItem(
            theme = theme,
            item = items[position],
            onItemChange = { item ->
                onItemChange(item, position)
            }
        )
    }

    override fun getItemCount(): Int = items.size

}