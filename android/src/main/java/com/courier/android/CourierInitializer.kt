package com.courier.android

import android.content.Context
import androidx.startup.Initializer

/**
 * Auto-initializes [Courier] at process startup via AndroidX Startup so that
 * the SDK is warm (OkHttp pool, lifecycle callbacks, cached state) before any
 * `FirebaseMessagingService` callback fires — especially important for
 * killed-state push delivery.
 *
 * Apps that need lazy init can opt out by adding to their manifest:
 * ```xml
 * <provider
 *     android:name="androidx.startup.InitializationProvider"
 *     android:authorities="${applicationId}.androidx-startup"
 *     tools:node="merge">
 *     <meta-data
 *         android:name="com.courier.android.CourierInitializer"
 *         tools:node="remove" />
 * </provider>
 * ```
 */
class CourierInitializer : Initializer<Courier> {

    override fun create(context: Context): Courier {
        Courier.initialize(context.applicationContext)
        return Courier.shared
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
