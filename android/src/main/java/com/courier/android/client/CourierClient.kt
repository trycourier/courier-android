package com.courier.android.client

import com.courier.android.BuildConfig

class CourierClient(
    jwt: String? = null,
    clientKey: String? = null,
    userId: String,
    connectionId: String? = null,
    tenantId: String? = null,
    apiUrls: ApiUrls = ApiUrls(),
    showLogs: Boolean = BuildConfig.DEBUG
) {

    companion object {
        internal const val TAG = "Courier SDK Client"
        val default = CourierClient(userId = "default")
    }

    data class ApiUrls(
        val rest: String = "https://api.courier.com",
        val graphql: String = "https://api.courier.com/client/q",
        val inboxGraphql: String = "https://inbox.courier.com/q",
        val inboxWebSocket: String = "wss://realtime.courier.com"
    )

    data class Options(
        val jwt: String?,
        val clientKey: String?,
        val userId: String,
        val connectionId: String?,
        val tenantId: String?,
        val apiUrls: ApiUrls,
        val showLogs: Boolean,
    )

    val options = Options(
        jwt = jwt,
        clientKey = clientKey,
        userId = userId,
        connectionId = connectionId,
        tenantId = tenantId,
        apiUrls = apiUrls,
        showLogs = showLogs,
    )

    val tokens by lazy { TokenClient(options) }
    val brands by lazy { BrandClient(options) }
    val inbox by lazy { InboxClient(options) }
    val preferences by lazy { PreferenceClient(options) }
    val tracking by lazy { TrackingClient(options) }

}