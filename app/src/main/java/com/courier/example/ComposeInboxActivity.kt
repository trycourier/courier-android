package com.courier.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.models.markAsArchived
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.ui.CourierStyles
import com.courier.android.ui.inbox.CourierInbox
import com.courier.android.ui.inbox.CourierInboxTheme

fun getTheme(context: ComponentActivity): CourierInboxTheme {

    val whiteColor = Color(0xFFFFFFFF).toArgb()
    val blackColor = Color(0xFF000000).toArgb()
    val blackLightColor = Color(0x80000000).toArgb()
    val primaryColor = Color(0xFF6650a4).toArgb()
    val primaryLightColor = Color(0xFF625b71).toArgb()
    val font = ResourcesCompat.getFont(context, R.font.poppins)

    return CourierInboxTheme(
        loadingIndicatorColor = primaryColor,
        tabIndicatorColor = primaryColor,
        tabStyle = CourierStyles.Inbox.TabStyle(
            selected = CourierStyles.Inbox.TabItemStyle(
                font = CourierStyles.Font(
                    typeface = font,
                    sizeInSp = 18,
                    color = primaryColor
                ),
                indicator = CourierStyles.Inbox.TabIndicatorStyle(
                    font = CourierStyles.Font(
                        typeface = font,
                        sizeInSp = 14,
                        color = whiteColor
                    ),
                    color = primaryColor
                )
            ),
            unselected = CourierStyles.Inbox.TabItemStyle(
                font = CourierStyles.Font(
                    typeface = font,
                    sizeInSp = 18,
                    color = blackLightColor,
                ),
                indicator = CourierStyles.Inbox.TabIndicatorStyle(
                    font = CourierStyles.Font(
                        typeface = font,
                        sizeInSp = 14,
                        color = whiteColor
                    ),
                    color = blackLightColor,
                )
            )
        ),
        readingSwipeActionStyle = CourierStyles.Inbox.ReadingSwipeActionStyle(
            read = CourierStyles.Inbox.SwipeActionStyle(
                color = primaryColor
            ),
            unread = CourierStyles.Inbox.SwipeActionStyle(
                color = primaryLightColor
            )
        ),
        unreadIndicatorStyle = CourierStyles.Inbox.UnreadIndicatorStyle(
            indicator = CourierStyles.Inbox.UnreadIndicator.DOT,
            color = primaryColor
        ),
        titleStyle = CourierStyles.Inbox.TextStyle(
            unread = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 18
            ),
            read = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 18
            ),
        ),
        bodyStyle = CourierStyles.Inbox.TextStyle(
            unread = CourierStyles.Font(
                typeface = font,
                color = blackLightColor,
                sizeInSp = 16
            ),
            read = CourierStyles.Font(
                typeface = font,
                color = blackLightColor,
                sizeInSp = 16
            )
        ),
        timeStyle = CourierStyles.Inbox.TextStyle(
            unread = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 14
            ),
            read = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 14
            )
        ),
        infoViewStyle = CourierStyles.InfoViewStyle(
            font = CourierStyles.Font(
                typeface = font,
                color = blackColor,
                sizeInSp = 18
            ),
            button = CourierStyles.Button(
                font = CourierStyles.Font(
                    typeface = font,
                    color = whiteColor,
                    sizeInSp = 16
                ),
                backgroundColor = primaryColor,
                cornerRadiusInDp = 100
            )
        ),
        buttonStyle = CourierStyles.Inbox.ButtonStyle(
            unread = CourierStyles.Button(
                font = CourierStyles.Font(
                    typeface = font,
                    color = whiteColor,
                    sizeInSp = 16
                ),
                backgroundColor = primaryColor,
                cornerRadiusInDp = 100
            ),
            read = CourierStyles.Button(
                font = CourierStyles.Font(
                    typeface = font,
                    color = whiteColor,
                    sizeInSp = 16
                ),
                backgroundColor = primaryColor,
                cornerRadiusInDp = 100
            )
        ),
        dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
    )
}

class ComposeInboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Courier.initialize(this)
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                CourierInbox(
                    canSwipePages = true,
                    lightTheme = getTheme(this),
                    darkTheme = getTheme(this),
                    modifier = Modifier.padding(innerPadding),
                    onClickMessageListener = { message, index ->
                        if (message.isRead) message.markAsUnread() else message.markAsRead()
                    },
                    onLongPressMessageListener = { message, index ->
                        message.markAsArchived()
                    }
                )
            }
        }
    }
}