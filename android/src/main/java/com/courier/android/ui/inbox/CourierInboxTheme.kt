package com.courier.android.ui.inbox

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.client.error
import com.courier.android.models.CourierBrand
import com.courier.android.models.CourierException
import com.courier.android.ui.CourierStyles

data class CourierInboxTheme(
    val brandId: String? = null,
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
    internal val dividerItemDecoration: DividerItemDecoration? = null,
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
                client?.options?.error(e.message)
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