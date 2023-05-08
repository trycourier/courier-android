package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.inbox.CourierInbox
import com.courier.android.inbox.CourierInboxButtonStyles
import com.courier.android.inbox.CourierInboxFont
import com.courier.android.inbox.CourierInboxTheme
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.modules.readAllInboxMessages
import com.courier.example.R

class StyledInboxFragment : Fragment(R.layout.fragment_styled_inbox) {

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
            unreadIndicatorBarColor = R.color.courier_red,
            loadingIndicatorColor = R.color.courier_purple,
            titleFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.black,
                sizeInSp = 18
            ),
            bodyFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.darker_gray,
                sizeInSp = 16
            ),
            timeFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.darker_gray,
                sizeInSp = 14
            ),
            detailTitleFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.black,
                sizeInSp = 18
            ),
            buttonStyles = CourierInboxButtonStyles(
                font = CourierInboxFont(
                    typeface = R.font.poppins,
                    color = android.R.color.white,
                    sizeInSp = 16
                ),
                backgroundColor = R.color.courier_purple,
                cornerRadiusInDp = 100
            ),
            dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )

        inbox.darkTheme = CourierInboxTheme(
            unreadIndicatorBarColor = R.color.courier_red,
            loadingIndicatorColor = R.color.courier_purple,
            titleFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.white,
                sizeInSp = 18
            ),
            bodyFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.white,
                sizeInSp = 16
            ),
            timeFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.white,
                sizeInSp = 14
            ),
            detailTitleFont = CourierInboxFont(
                typeface = R.font.poppins,
                color = android.R.color.white,
                sizeInSp = 18
            ),
            buttonStyles = CourierInboxButtonStyles(
                font = CourierInboxFont(
                    typeface = R.font.poppins,
                    color = android.R.color.white,
                    sizeInSp = 16
                ),
                backgroundColor = R.color.courier_red,
                cornerRadiusInDp = 0
            )
        )

        inbox.setOnClickMessageListener { message, index ->
            Courier.log(message.toString())
            Toast.makeText(context, "Message Click: $message", Toast.LENGTH_SHORT).show()
            if (message.isRead) message.markAsUnread() else message.markAsRead()
        }

        inbox.setOnClickActionListener { action, message, index ->
            Courier.log(action.toString())
            Toast.makeText(context, "Action Click: ${action.content}", Toast.LENGTH_SHORT).show()
        }

    }

}