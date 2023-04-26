package com.courier.example

import android.app.Application
import com.courier.android.Courier

class ExampleApp: Application() {

    init {

        // Initialize the Courier SDK
        // This is needed mostly for handling Shared Preferences
        Courier.initialize(context = this)

    }

}