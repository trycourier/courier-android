package com.courier.android.preferences

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.courier.android.R
import com.courier.android.models.CourierPreferenceStatus
import com.courier.android.models.CourierPreferenceTopic
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson

internal class PreferenceTopicBottomSheet(private val topic: CourierPreferenceTopic, private val onDismiss: (CourierPreferenceTopic) -> Unit) : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "PreferenceTopicBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.preference_topic_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.titleTextView)
        title.text = topic.topicName

        val textView = view.findViewById<TextView>(R.id.textView)
        textView.text = Gson().toJson(topic).toString()

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // TODO
        val newTopic = CourierPreferenceTopic(
            defaultStatus = topic.defaultStatus,
            hasCustomRouting = topic.hasCustomRouting,
            custom_routing = topic.customRouting.map { it.value },
            status = if (topic.status == CourierPreferenceStatus.OPTED_IN) CourierPreferenceStatus.OPTED_OUT else CourierPreferenceStatus.OPTED_IN,
            topicId = topic.topicId,
            topicName = topic.topicName,
            sectionId = topic.sectionId,
            sectionName = topic.sectionName
        )

        onDismiss(newTopic)

    }

    fun show(context: Context) {
        try {
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            show(fragmentManager, TAG)
        } catch (e: ClassCastException) {
            print(e.toString())
        }
    }

}