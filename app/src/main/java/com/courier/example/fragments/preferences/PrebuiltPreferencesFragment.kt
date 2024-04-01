package com.courier.example.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.courier.android.ui.preferences.CourierPreferences
import com.courier.example.R

class PrebuiltPreferencesFragment: Fragment(R.layout.fragment_prebuilt_preferences) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferences: CourierPreferences = view.findViewById(R.id.courierPreferences)

        preferences.apply {
            mode = CourierPreferences.Mode.Topic
            onError = { e ->
                print(e)
            }
        }

    }

}