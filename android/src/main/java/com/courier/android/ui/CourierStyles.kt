package com.courier.android.ui

import android.graphics.Typeface
import androidx.annotation.ColorInt

class CourierStyles {

    class Inbox {

        data class ButtonStyle(
            val unread: Button,
            val read: Button,
        )

        data class TextStyle(
            val unread: Font,
            val read: Font,
        )

        enum class UnreadIndicator {
            DOT,
            LINE
        }

        data class UnreadIndicatorStyle(
            val indicator: UnreadIndicator = UnreadIndicator.LINE,
            @ColorInt val color: Int? = null
        )

    }

    class Preferences {

        data class SettingStyles(
            val font: Font = Font(),
            @ColorInt val toggleThumbColor: Int? = null,
            @ColorInt val toggleTrackColor: Int? = null,
        )

    }

    data class Button(
        val font: Font? = null,
        @ColorInt val backgroundColor: Int? = null,
        val cornerRadiusInDp: Int? = null
    )

    data class Font(
        val typeface: Typeface? = null,
        @ColorInt val color: Int? = null,
        val sizeInSp: Int? = null,
    )

    data class InfoViewStyle(
        val font: Font,
        val button: Button,
    )

}