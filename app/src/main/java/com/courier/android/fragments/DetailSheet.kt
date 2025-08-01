package com.courier.android.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.courier.android.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

data class SheetAction(
    val title: String,
    val onClick: () -> Unit,
)

class SheetActionAdapter(
    private val context: Context,
    private val items: List<SheetAction>,
    private val onClick: (SheetAction) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): SheetAction = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.sheet_item, parent, false)
        val titleTextView = view.findViewById<TextView>(R.id.textView)
        val item = getItem(position)
        titleTextView.text = item.title
        view.setOnClickListener { onClick(item) }
        return view
    }
}

class DetailSheet(private val message: String, private val items: List<SheetAction> = emptyList()) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.detail_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.textView).text = message
        val listView = view.findViewById<ListView>(R.id.listView)
        val adapter = SheetActionAdapter(requireContext(), items, onClick = { item ->
            dismiss()
            item.onClick()
        })
        listView.adapter = adapter
        adjustListViewHeight(listView)
    }

    private fun adjustListViewHeight(listView: ListView) {
        val adapter = listView.adapter ?: return

        var totalHeight = 0
        for (i in 0 until adapter.count) {
            val listItem = adapter.getView(i, null, listView)
            listItem.measure(
                View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams
        params.height = totalHeight + (listView.dividerHeight * (adapter.count - 1))
        listView.layoutParams = params
    }

}