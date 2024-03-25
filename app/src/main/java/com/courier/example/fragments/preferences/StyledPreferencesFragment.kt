package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.courier.android.inbox.CourierInboxFont
import com.courier.android.inbox.CourierPreferencesSettingStyles
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.preferences.CourierPreferences
import com.courier.android.preferences.CourierPreferencesTheme
import com.courier.example.R

class StyledPreferencesFragment : Fragment(R.layout.fragment_styled_preferences) {

    private lateinit var preferences: CourierPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = view.findViewById(R.id.courierPreferences)
        preferences.mode = CourierPreferences.Mode.Channels(listOf(CourierPreferenceChannel.PUSH, CourierPreferenceChannel.SMS, CourierPreferenceChannel.EMAIL))

        val font = ResourcesCompat.getFont(requireContext(), R.font.poppins)
        val purple = ContextCompat.getColor(requireContext(), R.color.courier_purple)
        val purple200 = ContextCompat.getColor(requireContext(), R.color.purple_200)

        val theme = CourierPreferencesTheme(
            brandId = "7S9RBWHHS9MBYRPSRYAFYF9K3Q1M",
            sectionTitleFont = CourierInboxFont(
                typeface = font,
                color = purple,
                sizeInSp = 22,
            ),
            topicTitleFont = CourierInboxFont(
                typeface = font,
            ),
            topicSubtitleFont = CourierInboxFont(
                typeface = font,
                color = ContextCompat.getColor(requireContext(), android.R.color.darker_gray),
            ),
            sheetTitleFont = CourierInboxFont(
                typeface = font,
                color = purple,
                sizeInSp = 22,
            ),
            sheetSettingStyles = CourierPreferencesSettingStyles(
                font = CourierInboxFont(
                    typeface = font,
                ),
                toggleThumbColor = purple,
                toggleTrackColor = purple200,
            )
        )

        preferences.lightTheme = theme
        preferences.darkTheme = theme

    }

}