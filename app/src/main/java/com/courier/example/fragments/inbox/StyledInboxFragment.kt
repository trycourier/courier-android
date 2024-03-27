package com.courier.example.fragments.inbox

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.ui.CourierStyles
import com.courier.android.ui.inbox.CourierInbox
import com.courier.android.ui.inbox.CourierInboxTheme
import com.courier.example.R

class StyledInboxFragment : Fragment(R.layout.fragment_styled_inbox) {

    private lateinit var inbox: CourierInbox

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inbox = view.findViewById(R.id.courierInbox)

        val font = ResourcesCompat.getFont(requireContext(), R.font.poppins)

        inbox.lightTheme = CourierInboxTheme(
            brandId = "7S9RBWHHS9MBYRPSRYAFYF9K3Q1M",
            loadingIndicatorColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
            unreadIndicatorStyle = CourierStyles.Inbox.UnreadIndicatorStyle(
                indicator = CourierStyles.Inbox.UnreadIndicator.DOT,
                color = ContextCompat.getColor(requireContext(), R.color.courier_red)
            ),
            titleStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 18
                ),
                read = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 18
                ),
            ),
            bodyStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 16
                ),
                read = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 16
                )
            ),
            timeStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 14
                ),
                read = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 14
                )
            ),
            infoViewStyle = CourierStyles.InfoViewStyle(
                font = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.black),
                    sizeInSp = 18
                ),
                button = CourierStyles.Button(
                    font = CourierStyles.Font(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
                    cornerRadiusInDp = 100
                )
            ),
            buttonStyle = CourierStyles.Inbox.ButtonStyle(
                unread = CourierStyles.Button(
                    font = CourierStyles.Font(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.courier_purple),
                    cornerRadiusInDp = 100
                ),
                read = CourierStyles.Button(
                    font = CourierStyles.Font(
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
            brandId = "7S9RBWHHS9MBYRPSRYAFYF9K3Q1M",
            titleStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 18
                ),
                read = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
                    sizeInSp = 18
                ),
            ),
            bodyStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 16
                ),
                read = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 16
                )
            ),
            timeStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 14
                ),
                read = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 14
                )
            ),
            infoViewStyle = CourierStyles.InfoViewStyle(
                font = CourierStyles.Font(
                    typeface = font,
                    color = ContextCompat.getColor(requireContext(), android.R.color.white),
                    sizeInSp = 18
                ),
                button = CourierStyles.Button(
                    font = CourierStyles.Font(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    cornerRadiusInDp = 100
                )
            ),
            buttonStyle = CourierStyles.Inbox.ButtonStyle(
                unread = CourierStyles.Button(
                    font = CourierStyles.Font(
                        typeface = font,
                        color = ContextCompat.getColor(requireContext(), android.R.color.white),
                        sizeInSp = 16
                    ),
                    cornerRadiusInDp = 100
                ),
                read = CourierStyles.Button(
                    font = CourierStyles.Font(
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