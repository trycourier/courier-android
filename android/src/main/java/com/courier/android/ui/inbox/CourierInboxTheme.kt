package com.courier.android.ui.inbox

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.R
import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierException
import com.courier.android.ui.CourierStyles
import com.courier.android.utils.error

data class CourierInboxTheme(
    val brandId: String? = null,
    @ColorInt internal val tabIndicatorColor: Int? = null,
    internal val tabStyle: CourierStyles.Inbox.TabStyle = CourierStyles.Inbox.TabStyle(
        selected = CourierStyles.Inbox.TabItemStyle(
            font = CourierStyles.Font(
                sizeInSp = 18
            ),
            indicator = CourierStyles.Inbox.TabIndicatorStyle(
                font = CourierStyles.Font(
                    sizeInSp = 14
                ),
                color = null
            )
        ),
        unselected = CourierStyles.Inbox.TabItemStyle(
            font = CourierStyles.Font(
                sizeInSp = 18
            ),
            indicator = CourierStyles.Inbox.TabIndicatorStyle(
                font = CourierStyles.Font(
                    sizeInSp = 14
                ),
                color = null
            )
        )
    ),
    internal val readingSwipeActionStyle: CourierStyles.Inbox.ReadingSwipeActionStyle = CourierStyles.Inbox.ReadingSwipeActionStyle(),
    internal val archivingSwipeActionStyle: CourierStyles.Inbox.ArchivingSwipeActionStyle = CourierStyles.Inbox.ArchivingSwipeActionStyle(),
    @ColorInt private val loadingIndicatorColor: Int? = null,
    internal val unreadIndicatorStyle: CourierStyles.Inbox.UnreadIndicatorStyle = CourierStyles.Inbox.UnreadIndicatorStyle(),
    internal val titleStyle: CourierStyles.Inbox.TextStyle = CourierStyles.Inbox.TextStyle(
        unread = CourierStyles.Font(),
        read = CourierStyles.Font(),
    ),
    internal val timeStyle: CourierStyles.Inbox.TextStyle = CourierStyles.Inbox.TextStyle(
        unread = CourierStyles.Font(),
        read = CourierStyles.Font(),
    ),
    internal val bodyStyle: CourierStyles.Inbox.TextStyle = CourierStyles.Inbox.TextStyle(
        unread = CourierStyles.Font(),
        read = CourierStyles.Font(),
    ),
    internal val buttonStyle: CourierStyles.Inbox.ButtonStyle = CourierStyles.Inbox.ButtonStyle(
        unread = CourierStyles.Button(),
        read = CourierStyles.Button(),
    ),
    private val dividerItemDecoration: DividerItemDecoration? = null,
    internal val infoViewStyle: CourierStyles.InfoViewStyle = CourierStyles.InfoViewStyle(
        font = CourierStyles.Font(),
        button = CourierStyles.Button(),
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

        val client = Courier.shared.client

        // Get the new brand if needed
        brandId?.let {
            try {
                val res = client?.brands?.getBrand(it)
                brand = res?.data?.brand
            } catch (e: CourierException) {
                client?.error(e.message)
            }
        }

    }

    internal fun getDivider(context: Context): DividerItemDecoration {

        dividerItemDecoration?.let {
            return it
        }

        val transparentDrawable = ContextCompat.getDrawable(context, R.drawable.transparent_divider)
        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        transparentDrawable?.let {
            dividerItemDecoration.setDrawable(it)
        }

        return dividerItemDecoration

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
    internal fun getTabLayoutIndicatorColor(): Int? {

        if (tabIndicatorColor == null) {
            val value = brand?.settings?.colors?.primary
            return try { Color.parseColor(value) } catch (e: Exception) { null }
        }

        return tabIndicatorColor

    }

    @ColorInt
    internal fun getBadgeColor(isSelected: Boolean): Int? {

        val style = if (isSelected) tabStyle.selected else tabStyle.unselected

        if (style.indicator.color == null) {
            val value = brand?.settings?.colors?.primary
            return try { Color.parseColor(value) } catch (e: Exception) { null }
        }

        return style.indicator.color

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