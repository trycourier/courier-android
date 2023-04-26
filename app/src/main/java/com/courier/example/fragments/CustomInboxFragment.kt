package com.courier.example.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.models.CourierInboxListener
import com.courier.android.models.remove
import com.courier.android.modules.addInboxListener
import com.courier.example.R

class CustomInboxFragment: Fragment(R.layout.fragment_custom_inbox) {

    private lateinit var inboxListener: CourierInboxListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inboxListener = Courier.shared.addInboxListener(
            onInitialLoad = {
                print("Loading")
            },
            onError = { e ->
                print(e)
            },
            onMessagesChanged = { messages, unreadMessageCount, totalMessageCount, canPaginate ->
                print(totalMessageCount)
            }
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        inboxListener.remove()
    }

}