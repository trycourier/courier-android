package com.courier.android.models

import com.courier.android.Courier
import com.courier.android.modules.removeAuthenticationListener

class CourierAuthenticationListener(val onChange: (String?) -> Unit)

/**
 * Extensions
 */

fun CourierAuthenticationListener.remove() {
    Courier.shared.removeAuthenticationListener(listener = this)
}