package com.courier.android.modules

import com.courier.android.Courier
import com.courier.android.models.CourierProvider
import com.courier.android.repositories.InboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class CoreInbox {

    private val inboxRepo by lazy { InboxRepository() }

    suspend fun start() {

        if (Courier.shared.clientKey == null || Courier.shared.userId == null) {
            print("No auth found")
            return
        }

        val test = inboxRepo.getAllMessages(
            clientKey = Courier.shared.clientKey!!,
            userId = Courier.shared.userId!!,
            paginationLimit = 24
        )

        print(test)

    }

}

/**
 * Extensions
 */

fun Courier.addInboxListener() = Courier.coroutineScope.launch(Dispatchers.IO) {
    inbox.start() // TODO
}