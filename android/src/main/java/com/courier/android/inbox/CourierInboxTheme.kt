package com.courier.android.inbox

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.courier.android.R

data class CourierInboxTheme(
    internal val brandId: String? = null,
//    internal val messageAnimationStyle: UITableView.RowAnimation,
    @ColorRes private val unreadIndicatorBarColor: Int? = null,
//    private val loadingIndicatorColor: UIColor?,
//    internal val titleFont: CourierInboxFont,
//    internal val timeFont: CourierInboxFont,
//    internal val bodyFont: CourierInboxFont,
//    internal val detailTitleFont: CourierInboxFont,
//    internal val buttonStyles: CourierInboxButtonStyles,
//    internal val cellStyles: CourierInboxCellStyles,
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

}