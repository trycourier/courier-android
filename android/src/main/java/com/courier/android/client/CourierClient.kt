package com.courier.android.client

class CourierClient(jwt: String? = null, clientKey: String? = null, userId: String, connectionId: String? = null, tenantId: String? = null, showLogs: Boolean) {

    data class Options(val jwt: String?, val clientKey: String?, val userId: String, val connectionId: String?, val tenantId: String?, val showLogs: Boolean)

    private val options = Options(jwt, clientKey, userId, tenantId, connectionId, showLogs)

    val tokens by lazy { TokenClient(options) }
    val brands by lazy { BrandClient(options) }
    val inbox by lazy { InboxClient(options) }
    val preferences by lazy { PreferenceClient(options) }
    val tracking by lazy { TrackingClient(options) }

}