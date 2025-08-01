package com.courier.android.fragments.preferences

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.courier.android.Courier
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.ui.CourierStyles
import com.courier.android.ui.preferences.CourierPreferences
import com.courier.android.ui.preferences.CourierPreferencesTheme
import com.courier.android.utils.log
import com.courier.android.Env
import com.courier.android.R
import com.courier.android.Theme

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

        val theme = CourierPreferencesTheme(
            brandId = Env.COURIER_BRAND_ID,
            sectionTitleFont = CourierStyles.Font(
                typeface = Theme.getFont(requireContext()),
                color = Theme.getPrimaryColor(requireContext()),
                sizeInSp = Theme.getTitleFontSize(),
            ),
            topicDividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
            topicTitleFont = CourierStyles.Font(
                typeface = Theme.getFont(requireContext()),
            ),
            topicSubtitleFont = CourierStyles.Font(
                typeface = Theme.getFont(requireContext()),
                color = Theme.getSubtitleColor(requireContext()),
            ),
            sheetTitleFont = CourierStyles.Font(
                typeface = Theme.getFont(requireContext()),
                color = Theme.getPrimaryColor(requireContext()),
                sizeInSp = Theme.getTitleFontSize(),
            ),
            sheetDividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
            sheetSettingStyles = CourierStyles.Preferences.SettingStyles(
                font = CourierStyles.Font(
                    typeface = Theme.getFont(requireContext()),
                ),
                toggleThumbColor = Theme.getPrimaryColor(requireContext()),
                toggleTrackColor = Theme.getPrimaryLightColor(requireContext()),
            ),
            infoViewStyle = Theme.getInfoViewStyles(requireContext())
        )

        preferences.lightTheme = theme
        preferences.darkTheme = theme

        preferences.setOnScrollPreferencesListener { offsetInDp ->
            Courier.shared.client?.log(offsetInDp.toString())
        }

    }

}