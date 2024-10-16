package com.courier.example.fragments.inbox

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.models.markAsClicked
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.ui.inbox.CourierInbox
import com.courier.android.utils.log
import com.courier.example.R
import com.courier.example.toJson

class PrebuiltInboxFragment: Fragment(R.layout.fragment_prebuilt_inbox) {

    private lateinit var inbox: CourierInbox

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inbox = view.findViewById(R.id.courierInbox)

        inbox.setOnClickMessageListener { message, index ->
            val str = message.toJson() ?: "Invalid"
            Courier.shared.client?.log(str)
            if (message.isRead) message.markAsUnread() else message.markAsRead()
        }

        inbox.setOnClickActionListener { action, message, index ->
            val str = action.toJson() ?: "Invalid"
            Courier.shared.client?.log(str)
            action.markAsClicked(message.messageId)
        }

    }

}