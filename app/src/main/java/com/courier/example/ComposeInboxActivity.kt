package com.courier.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.ui.Modifier
import com.courier.android.Courier
import com.courier.android.models.markAsArchived
import com.courier.android.models.markAsRead
import com.courier.android.models.markAsUnread
import com.courier.android.ui.inbox.CourierInbox

class ComposeInboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Courier.initialize(this)
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                CourierInbox(
                    canSwipePages = true,
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