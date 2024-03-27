package com.courier.android.ui.preferences

import android.graphics.Color
import androidx.annotation.ColorInt
import com.courier.android.Courier
import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierException
import com.courier.android.modules.getBrand
import com.courier.android.ui.CourierStyles

data class CourierPreferencesTheme(
    val brandId: String? = null,
    @ColorInt private val loadingIndicatorColor: Int? = null,
    val sectionTitleFont: CourierStyles.Font = CourierStyles.Font(),
    val topicTitleFont: CourierStyles.Font = CourierStyles.Font(),
    val topicSubtitleFont: CourierStyles.Font = CourierStyles.Font(),
    val sheetTitleFont: CourierStyles.Font = CourierStyles.Font(),
    val sheetSettingStyles: CourierStyles.Preferences.SettingStyles = CourierStyles.Preferences.SettingStyles(),
) {
    companion object {
        val DEFAULT_LIGHT = CourierPreferencesTheme()
        val DEFAULT_DARK = CourierPreferencesTheme()
    }

    internal var brand: CourierBrand? = null

    internal suspend fun getBrandIfNeeded() {

        // Reset brand
        brand = null

        // Get the new brand if needed
        brandId?.let {
            try {
                brand = Courier.shared.getBrand(it)
            } catch (e: CourierException) {
                Courier.error(e.message)
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