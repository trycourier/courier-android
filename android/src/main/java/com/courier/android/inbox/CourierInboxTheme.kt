package com.courier.android.inbox

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.models.CourierBrand

data class CourierInboxTheme(
    @ColorInt private val loadingIndicatorColor: Int? = null,
    internal val unreadIndicatorStyle: CourierInboxUnreadIndicatorStyle = CourierInboxUnreadIndicatorStyle(),
    internal val titleFont: CourierInboxFont = CourierInboxFont(),
    internal val timeFont: CourierInboxFont = CourierInboxFont(),
    internal val bodyFont: CourierInboxFont = CourierInboxFont(),
    internal val detailTitleFont: CourierInboxFont = CourierInboxFont(),
    internal val buttonStyles: CourierInboxButtonStyles = CourierInboxButtonStyles(),
    internal val dividerItemDecoration: DividerItemDecoration? = null,
) {

    companion object {
        val DEFAULT_LIGHT = CourierInboxTheme()
        val DEFAULT_DARK = CourierInboxTheme()
    }

    private var brand: CourierBrand? = null

    @ColorInt
    internal fun getUnreadColor(): Int? {

        if (unreadIndicatorStyle.color == null) {
            val value = brand?.settings?.colors?.primary
            return try { Color.parseColor(value) } catch (e: Exception) { null }
        }

        return unreadIndicatorStyle.color

    }

    @ColorInt
    internal fun getLoadingColor(): Int? {

        if (loadingIndicatorColor == null) {
            val value = brand?.settings?.colors?.primary
            return try { Color.parseColor(value) } catch (e: Exception) { null }
        }

        return loadingIndicatorColor

    }

    @ColorInt
    internal fun getButtonColor(): Int? {

        if (buttonStyles.backgroundColor == null) {
            val value = brand?.settings?.colors?.primary
            return try { Color.parseColor(value) } catch (e: Exception) { null }
        }

        return buttonStyles.backgroundColor

    }

    internal fun attachBrand(brand: CourierBrand) {
        this.brand = brand
    }

}

data class CourierInboxButtonStyles(
    val font: CourierInboxFont? = null,
    @ColorInt val backgroundColor: Int? = null,
    val cornerRadiusInDp: Int? = null
)

data class CourierInboxFont(
    val typeface: Typeface? = null,
    @ColorInt val color: Int? = null,
    val sizeInSp: Int? = null,
)

enum class CourierInboxUnreadIndicator {
    DOT,
    LINE
}

data class CourierInboxUnreadIndicatorStyle(
    val indicator: CourierInboxUnreadIndicator = CourierInboxUnreadIndicator.LINE,
    @ColorInt val color: Int? = null
)