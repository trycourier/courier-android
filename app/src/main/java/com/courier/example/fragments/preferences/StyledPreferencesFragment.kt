package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.ui.CourierStyles
import com.courier.android.ui.preferences.CourierPreferences
import com.courier.android.ui.preferences.CourierPreferencesTheme
import com.courier.example.Env
import com.courier.example.R

class StyledPreferencesFragment : Fragment(R.layout.fragment_styled_preferences) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences: CourierPreferences = view.findViewById(R.id.courierPreferences)

        preferences.apply {

            val availableChannels = listOf(
                CourierPreferenceChannel.PUSH,
                CourierPreferenceChannel.SMS,
                CourierPreferenceChannel.EMAIL
            )

            mode = CourierPreferences.Mode.Channels(availableChannels)

            onError = { e ->
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show()
            }

        }

        val font = ResourcesCompat.getFont(requireContext(), R.font.poppins)
        val purple = ContextCompat.getColor(requireContext(), R.color.courier_purple)
        val purple200 = ContextCompat.getColor(requireContext(), R.color.purple_200)
        val lightGrey = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)

        val theme = CourierPreferencesTheme(
            brandId = Env.COURIER_BRAND_ID,
            loadingIndicatorColor = purple,
            sectionTitleFont = CourierStyles.Font(
                typeface = font,
                color = purple,
                sizeInSp = 22,
            ),
            topicDividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
            topicTitleFont = CourierStyles.Font(
                typeface = font,
            ),
            topicSubtitleFont = CourierStyles.Font(
                typeface = font,
                color = lightGrey,
            ),
            sheetTitleFont = CourierStyles.Font(
                typeface = font,
                color = purple,
                sizeInSp = 22,
            ),
            sheetDividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
            sheetSettingStyles = CourierStyles.Preferences.SettingStyles(
                font = CourierStyles.Font(
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