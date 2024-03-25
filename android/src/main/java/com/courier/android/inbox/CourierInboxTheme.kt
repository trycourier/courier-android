package com.courier.android.inbox

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierException
import com.courier.android.modules.getBrand

data class CourierInboxTheme(
    val brandId: String? = null,
    @ColorInt private val loadingIndicatorColor: Int? = null,
    internal val unreadIndicatorStyle: CourierInboxUnreadIndicatorStyle = CourierInboxUnreadIndicatorStyle(),
    internal val titleStyle: CourierInboxTextStyle = CourierInboxTextStyle(
        unread = CourierInboxFont(),
        read = CourierInboxFont(),
    ),
    internal val timeStyle: CourierInboxTextStyle = CourierInboxTextStyle(
        unread = CourierInboxFont(),
        read = CourierInboxFont(),
    ),
    internal val bodyStyle: CourierInboxTextStyle = CourierInboxTextStyle(
        unread = CourierInboxFont(),
        read = CourierInboxFont(),
    ),
    internal val buttonStyle: CourierInboxButtonStyle = CourierInboxButtonStyle(
        unread = CourierInboxButton(),
        read = CourierInboxButton(),
    ),
    internal val dividerItemDecoration: DividerItemDecoration? = null,
    internal val infoViewStyle: CourierInboxInfoViewStyle = CourierInboxInfoViewStyle(
        font = CourierInboxFont(),
        button = CourierInboxButton(),
    ),
) {

    companion object {
        val DEFAULT_LIGHT = CourierInboxTheme()
        val DEFAULT_DARK = CourierInboxTheme()
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
    internal fun getButtonColor(isRead: Boolean): Int? {

        val styleColor = if (isRead) buttonStyle.read.backgroundColor else buttonStyle.unread.backgroundColor
        val brandColor = brand?.settings?.colors?.primary

        if (styleColor != null) {
            return styleColor
        }

        if (brandColor != null) {
            return try { Color.parseColor(brandColor) } catch (e: Exception) { null }
        }

        return if (isRead) buttonStyle.read.backgroundColor else buttonStyle.unread.backgroundColor

    }

}

data class CourierInboxButtonStyle(
    val unread: CourierInboxButton,
    val read: CourierInboxButton,
)

data class CourierInboxButton(
    val font: CourierInboxFont? = null,
    @ColorInt val backgroundColor: Int? = null,
    val cornerRadiusInDp: Int? = null
)

data class CourierInboxFont(
    val typeface: Typeface? = null,
    @ColorInt val color: Int? = null,
    val sizeInSp: Int? = null,
)

data class CourierPreferencesSettingStyles(
    val font: CourierInboxFont = CourierInboxFont(),
    @ColorInt val toggleThumbColor: Int? = null,
    @ColorInt val toggleTrackColor: Int? = null,
)

data class CourierInboxTextStyle(
    val unread: CourierInboxFont,
    val read: CourierInboxFont,
)

data class CourierInboxInfoViewStyle(
    val font: CourierInboxFont,
    val button: CourierInboxButton,
)

enum class CourierInboxUnreadIndicator {
    DOT,
    LINE
}

data class CourierInboxUnreadIndicatorStyle(
    val indicator: CourierInboxUnreadIndicator = CourierInboxUnreadIndicator.LINE,
    @ColorInt val color: Int? = null
)