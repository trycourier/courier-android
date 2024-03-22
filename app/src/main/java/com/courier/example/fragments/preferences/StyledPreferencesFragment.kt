package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.courier.android.Courier
import com.courier.android.models.CourierPreferenceChannel
import com.courier.android.preferences.CourierPreferences
import com.courier.example.R

class StyledPreferencesFragment: Fragment(R.layout.fragment_styled_preferences) {

    private lateinit var preferences: CourierPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferences = view.findViewById(R.id.courierPreferences)
        preferences.mode = CourierPreferences.Mode.Channels(listOf(CourierPreferenceChannel.PUSH, CourierPreferenceChannel.SMS, CourierPreferenceChannel.EMAIL))

    }

}