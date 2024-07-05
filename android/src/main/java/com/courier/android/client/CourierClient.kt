package com.courier.android.client

import com.courier.android.BuildConfig
import com.courier.android.utils.Logger

class CourierClient(
    jwt: String? = null,
    clientKey: String? = null,
    userId: String,
    connectionId: String? = null,
    tenantId: String? = null,
    showLogs: Boolean = BuildConfig.DEBUG
) {

    companion object {
        internal const val TAG = "Courier SDK Client"
    }

    data class Options(
        val jwt: String?,
        val clientKey: String?,
        val userId: String,
        val connectionId: String?,
        val tenantId: String?,
        val showLogs: Boolean
    )

    val options = Options(
        jwt = jwt,
        clientKey = clientKey,
        userId = userId,
        connectionId = connectionId,
        tenantId = tenantId,
        showLogs = showLogs,
    )

    val tokens by lazy { TokenClient(options) }
    val brands by lazy { BrandClient(options) }
    val inbox by lazy { InboxClient(options) }
    val preferences by lazy { PreferenceClient(options) }
    val tracking by lazy { TrackingClient(options) }

}

fun CourierClient.Options.log(data: String) {
    if (showLogs) {
        Logger.log(data)
    }
}

fun CourierClient.Options.warn(data: String) {
    if (showLogs) {
        Logger.warn(data)
    }
}

internal fun CourierClient.Options.error(data: String?) {
    if (showLogs) {
        val message = data ?: "Oops, an error occurred"
        Logger.error(message)
    }
}