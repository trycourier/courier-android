package com.courier.android.models

import com.courier.android.Courier

class CourierAuthenticationListener(val onChange: (String?) -> Unit)

/**
 * Extensions
 */

fun CourierAuthenticationListener.remove() {
    Courier.shared.auth.removeAuthenticationListener(listener = this)
}