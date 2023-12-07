package com.courier.example.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.inbox.*
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

        val font = ResourcesCompat.getFont(requireContext(), R.font.poppins)

        inbox.lightTheme = CourierInboxTheme(
            loadingIndicatorColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
            unreadIndicatorStyle = CourierInboxUnreadIndicatorStyle(
                indicator = CourierInboxUnreadIndicator.DOT,
                color = ContextCompat.getColor(requireContext(), R.color.courier_red)
            ),
            titleStyle = CourierInboxTextStyle(
                unread = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 18
                ),
                read = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 18
                ),
            ),
            bodyStyle = CourierInboxTextStyle(
                unread = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 16
                ),
                read = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 16
                )
            ),
            timeStyle = CourierInboxTextStyle(
                unread = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 14
                ),
                read = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 14
                )
            ),
            infoViewStyle = CourierInboxFont(
                typeface = font,
                color = ContextCompat.getColor(requireContext(), android.R.color.black),
                sizeInSp = 18
            ),
            buttonStyle = CourierInboxButtonStyles(
                unread = CourierInboxButtonTheme(
                    font = CourierInboxFont(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
                    cornerRadiusInDp = 100
                ),
                read = CourierInboxButtonTheme(
                    font = CourierInboxFont(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
                    cornerRadiusInDp = 100
                )
            ),
            dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )

        inbox.darkTheme = CourierInboxTheme(
            titleStyle = CourierInboxTextStyle(
                unread = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 18
                ),
                read = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 18
                ),
            ),
            bodyStyle = CourierInboxTextStyle(
                unread = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 16
                ),
                read = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 16
                )
            ),
            timeStyle = CourierInboxTextStyle(
                unread = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 14
                ),
                read = CourierInboxFont(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 14
                )
            ),
            infoViewStyle = CourierInboxFont(
                typeface = font,
                color = ContextCompat.getColor(requireContext(), android.R.color.white),
                sizeInSp = 18
            ),
            buttonStyle = CourierInboxButtonStyles(
                unread = CourierInboxButtonTheme(
                    font = CourierInboxFont(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    cornerRadiusInDp = 100
                ),
                read = CourierInboxButtonTheme(
                    font = CourierInboxFont(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    cornerRadiusInDp = 0
                )
            ),
            dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
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

        inbox.setOnScrollInboxListener { offsetInDp ->
            Courier.log(offsetInDp.toString())
        }

    }

}