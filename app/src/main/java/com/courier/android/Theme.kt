package com.courier.android

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.courier.android.ui.CourierStyles

class Theme {

    companion object {
        fun getPrimaryColor(context: Context) = ContextCompat.getColor(context, R.color.courier_purple)
        fun getPrimaryLightColor(context: Context) = ContextCompat.getColor(context, R.color.courier_purple_light)
        fun getWhiteColor(context: Context) = ContextCompat.getColor(context, R.color.white)
        fun getSubtitleColor(context: Context) = ContextCompat.getColor(context, android.R.color.darker_gray)
        fun getFont(context: Context) = ResourcesCompat.getFont(context, R.font.poppins)
        fun getSmallFontSize() = 14
        fun getTitleFontSize() = 22
        fun getInfoViewStyles(context: Context) = CourierStyles.InfoViewStyle(
            font = CourierStyles.Font(
                typeface = getFont(context),
                sizeInSp = 18
            ),
            button = getButton(context)
        )
        fun getButton(context: Context) = CourierStyles.Button(
            font = CourierStyles.Font(
                typeface = getFont(context),
                color = getWhiteColor(context),
            ),
            backgroundColor = getPrimaryColor(context),
            cornerRadiusInDp = 100
        )
    }

}