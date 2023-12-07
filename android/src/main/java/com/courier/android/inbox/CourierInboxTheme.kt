package com.courier.android.inbox

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.models.CourierBrand

data class CourierInboxTheme(
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

    internal fun attachBrand(brand: CourierBrand) {
        this.brand = brand
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