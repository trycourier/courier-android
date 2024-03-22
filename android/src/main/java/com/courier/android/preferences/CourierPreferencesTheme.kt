package com.courier.android.preferences

data class CourierPreferencesTheme(
    val brandId: String? = null
) {

    companion object {
        val DEFAULT_LIGHT = CourierPreferencesTheme()
        val DEFAULT_DARK = CourierPreferencesTheme()
    }

}