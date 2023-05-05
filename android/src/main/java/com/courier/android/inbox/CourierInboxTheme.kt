package com.courier.android.inbox

import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.recyclerview.widget.DividerItemDecoration

data class CourierInboxTheme(
    internal val brandId: String? = null,
//    internal val messageAnimationStyle: UITableView.RowAnimation,
    @ColorRes private val unreadIndicatorBarColor: Int? = null,
    @ColorRes private val loadingIndicatorColor: Int? = null,
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

    internal fun getUnreadColor(): Int? {

        // TODO: Handle brand
        if (unreadIndicatorBarColor == null) {
            return null
        }

        return unreadIndicatorBarColor

    }

    internal fun getLoadingColor(): Int? {

        // TODO: Handle brand
        if (loadingIndicatorColor == null) {
            return null
        }

        return loadingIndicatorColor

    }

}

data class CourierInboxButtonStyles(
    val font: CourierInboxFont? = null,
    @ColorRes val backgroundColor: Int? = null,
    val cornerRadiusInDp: Int? = null
)

data class CourierInboxFont(
    @FontRes val typeface: Int? = null,
    @ColorRes val color: Int? = null,
    val sizeInSp: Int? = null,
)