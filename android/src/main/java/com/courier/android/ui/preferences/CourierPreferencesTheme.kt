package com.courier.android.ui.preferences

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.client.error
import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierException
import com.courier.android.ui.CourierStyles

data class CourierPreferencesTheme(
    val brandId: String? = null,
    @ColorInt private val loadingIndicatorColor: Int? = null,
    val sectionTitleFont: CourierStyles.Font = CourierStyles.Font(),
    val topicDividerItemDecoration: DividerItemDecoration? = null,
    val topicTitleFont: CourierStyles.Font = CourierStyles.Font(),
    val topicSubtitleFont: CourierStyles.Font = CourierStyles.Font(),
    val sheetTitleFont: CourierStyles.Font = CourierStyles.Font(),
    val sheetDividerItemDecoration: DividerItemDecoration? = null,
    val sheetSettingStyles: CourierStyles.Preferences.SettingStyles = CourierStyles.Preferences.SettingStyles(),
    val infoViewStyle: CourierStyles.InfoViewStyle = CourierStyles.InfoViewStyle(
        font = CourierStyles.Font(),
        button = CourierStyles.Button(),
    ),
) {
    companion object {
        val DEFAULT_LIGHT = CourierPreferencesTheme()
        val DEFAULT_DARK = CourierPreferencesTheme()
    }

    internal var brand: CourierBrand? = null

    internal suspend fun getBrandIfNeeded() {

        // Reset brand
        brand = null

        val client = Courier.shared.client

        // Get the new brand if needed
        brandId?.let {
            try {
                val res = client?.brands?.getBrand(it)
                brand = res?.data?.brand
            } catch (e: CourierException) {
                client?.options?.error(e.message)
            }
        }

    }

    @ColorInt
    internal fun getLoadingColor(): Int? {

        if (loadingIndicatorColor == null) {
            val value = brand?.settings?.colors?.primary
            return try { Color.parseColor(value) } catch (e: Exception) { null }
        }

        return loadingIndicatorColor

    }

}