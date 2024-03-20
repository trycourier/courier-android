package com.courier.example.fragments.inbox

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.inbox.CourierInbox
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.example.R

class PrebuiltInboxFragment: Fragment(R.layout.fragment_prebuilt_inbox) {

    private lateinit var inbox: CourierInbox

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inbox = view.findViewById(R.id.courierInbox)

        inbox.setOnClickMessageListener { message, index ->
            Courier.log(message.toString())
            if (message.isRead) message.markAsUnread() else message.markAsRead()
        }

        inbox.setOnClickActionListener { action, message, index ->
            Courier.log(action.toString())
        }

    }

}