package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.inbox.CourierInbox
import com.courier.android.inbox.CourierInboxTheme
import com.courier.android.inbox.scrollToTop
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.modules.readAllInboxMessages
import com.courier.example.R

class StyledInboxFragment: Fragment(R.layout.fragment_styled_inbox) {

    private lateinit var inbox: CourierInbox

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the menu
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            return@setOnMenuItemClickListener if (item.itemId == R.id.readAll) {

                Courier.shared.readAllInboxMessages(
                    onSuccess = {
                        print("Messages are read")
                    },
                    onFailure = { error ->
                        print(error)
                    }
                )

                true
            } else {
                false
            }
        }

        inbox = view.findViewById(R.id.courierInbox)

        inbox.lightTheme = CourierInboxTheme(
            unreadIndicatorBarColor = android.R.color.holo_red_dark
        )

        inbox.darkTheme = CourierInboxTheme(
            unreadIndicatorBarColor = android.R.color.holo_blue_bright
        )

        inbox.setOnClickMessageListener { message, index ->
            Courier.log(message.toString())
            if (message.isRead) message.markAsUnread() else message.markAsRead()
        }

        inbox.setOnClickActionListener { action, message, index ->
            Courier.log(action.toString())
        }

    }

}