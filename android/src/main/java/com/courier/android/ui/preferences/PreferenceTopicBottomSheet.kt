package com.courier.android.ui.preferences

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.models.CourierPreferenceTopic
import com.courier.android.utils.isDarkModeOn
import com.courier.android.utils.setCourierFont
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


internal class PreferenceTopicBottomSheet(private val theme: CourierPreferencesTheme, private val topic: CourierPreferenceTopic, private val items: List<CourierSheetItem>, private val onDismiss: (List<CourierSheetItem>) -> Unit) : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "PreferenceTopicBottomSheet"
    }

    private var newItems = this.items.toMutableList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.courier_preferences_topic_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.titleTextView)
        title.text = topic.topicName

        val res = if (isDarkModeOn(view.context)) android.R.color.white else android.R.color.black
        val fallbackColor = ContextCompat.getColor(view.context, res)

        // Set the font
        title.setCourierFont(font = theme.sheetTitleFont, fallbackColor = fallbackColor)

        val adapter = PreferencesSheetItemAdapter(
            theme = theme,
            items = items,
            onItemChange = { newItem, index ->
                newItems[index] = newItem
            }
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = adapter

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss(newItems.toList())
    }

    fun show(context: Context) {
        try {
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            show(fragmentManager, TAG)
        } catch (e: ClassCastException) {
            Courier.log(e.toString())
        }
    }

}