package com.courier.example.fragments.inbox

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.ui.CourierStyles
import com.courier.android.ui.inbox.CourierInbox
import com.courier.android.ui.inbox.CourierInboxTheme
import com.courier.android.utils.log
import com.courier.example.Env
import com.courier.example.R
import com.courier.example.Theme
import com.courier.example.fragments.DetailSheet
import com.courier.example.toJson

class StyledInboxFragment : Fragment(R.layout.fragment_styled_inbox) {

    private lateinit var inbox: CourierInbox

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inbox = view.findViewById(R.id.courierInbox)

//        inbox.canSwipePages = true

        val theme = CourierInboxTheme(
            brandId = Env.COURIER_BRAND_ID,
            tabIndicatorColor = Theme.getPrimaryColor(requireContext()),
            tabStyle = CourierStyles.Inbox.TabStyle(
                selected = CourierStyles.Inbox.TabItemStyle(
                    font = CourierStyles.Font(
                        typeface = Theme.getFont(requireContext()),
                        sizeInSp = 18,
                        color = Theme.getPrimaryColor(requireContext())
                    ),
                    indicator = CourierStyles.Inbox.TabIndicatorStyle(
                        font = CourierStyles.Font(
                            typeface = Theme.getFont(requireContext()),
                            sizeInSp = 14,
                            color = Theme.getWhiteColor(requireContext())
                        ),
                        color = Theme.getPrimaryColor(requireContext())
                    )
                ),
                unselected = CourierStyles.Inbox.TabItemStyle(
                    font = CourierStyles.Font(
                        typeface = Theme.getFont(requireContext()),
                        sizeInSp = 18,
                        color = Theme.getSubtitleColor(requireContext())
                    ),
                    indicator = CourierStyles.Inbox.TabIndicatorStyle(
                        font = CourierStyles.Font(
                            typeface = Theme.getFont(requireContext()),
                            sizeInSp = 14,
                            color = Theme.getWhiteColor(requireContext())
                        ),
                        color = Theme.getSubtitleColor(requireContext())
                    )
                )
            ),
            readingSwipeActionStyle = CourierStyles.Inbox.ReadingSwipeActionStyle(
                read = CourierStyles.Inbox.SwipeActionStyle(
                    color = Theme.getPrimaryColor(requireContext())
                ),
                unread = CourierStyles.Inbox.SwipeActionStyle(
                    color = Theme.getPrimaryLightColor(requireContext())
                )
            ),
            unreadIndicatorStyle = CourierStyles.Inbox.UnreadIndicatorStyle(
                indicator = CourierStyles.Inbox.UnreadIndicator.DOT,
                color = Theme.getPrimaryColor(requireContext())
            ),
            titleStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                ),
                read = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                ),
            ),
            bodyStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                    color = Theme.getSubtitleColor(requireContext()),
                ),
                read = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                    color = Theme.getSubtitleColor(requireContext()),
                )
            ),
            timeStyle = CourierStyles.Inbox.TextStyle(
                unread = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                    sizeInSp = Theme.getSmallFontSize()
                ),
                read = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                    color = Theme.getSubtitleColor(requireContext()),
                    sizeInSp = Theme.getSmallFontSize()
                )
            ),
            infoViewStyle = Theme.getInfoViewStyles(requireContext()),
            buttonStyle = CourierStyles.Inbox.ButtonStyle(
                unread = Theme.getButton(requireContext()),
                read = Theme.getButton(requireContext())
            ),
            dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )

        inbox.lightTheme = theme
        inbox.darkTheme = theme

        inbox.setOnClickMessageListener { message, index ->
            val str = message.toJson() ?: "Invalid"
            Courier.shared.client?.log(str)
            if (message.isRead) message.markAsUnread() else message.markAsRead()
        }

        inbox.setOnClickActionListener { action, message, index ->
            val str = action.toJson() ?: "Invalid"
            DetailSheet(str).show(childFragmentManager, null)
        }

        inbox.setOnScrollInboxListener { offsetInDp ->
            Courier.shared.client?.log(offsetInDp.toString())
        }

    }

}