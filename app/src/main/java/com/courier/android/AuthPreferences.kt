package com.courier.android

import android.content.Context
import android.content.SharedPreferences
import com.courier.android.client.CourierClient
import androidx.core.content.edit

class AuthPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var tenantId: String?
        get() = prefs.getString(KEY_TENANT_ID, null)
        set(value) = prefs.edit { putString(KEY_TENANT_ID, value).apply() }

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(value) = prefs.edit { putString(KEY_API_KEY, value).apply() }

    var restUrl: String?
        get() = prefs.getString(KEY_REST_URL, null)
        set(value) = prefs.edit { putString(KEY_REST_URL, value).apply() }

    var graphqlUrl: String?
        get() = prefs.getString(KEY_GRAPHQL_URL, null)
        set(value) = prefs.edit { putString(KEY_GRAPHQL_URL, value).apply() }

    var inboxGraphqlUrl: String?
        get() = prefs.getString(KEY_INBOX_GRAPHQL_URL, null)
        set(value) = prefs.edit { putString(KEY_INBOX_GRAPHQL_URL, value).apply() }

    var inboxWebSocketUrl: String?
        get() = prefs.getString(KEY_INBOX_WEBSOCKET_URL, null)
        set(value) = prefs.edit { putString(KEY_INBOX_WEBSOCKET_URL, value).apply() }

    fun saveApiUrls(apiUrls: CourierClient.ApiUrls) {
        prefs.edit {
            putString(KEY_REST_URL, apiUrls.rest)
            putString(KEY_GRAPHQL_URL, apiUrls.graphql)
            putString(KEY_INBOX_GRAPHQL_URL, apiUrls.inboxGraphql)
            putString(KEY_INBOX_WEBSOCKET_URL, apiUrls.inboxWebSocket)
        }
    }

    fun getApiUrls(): CourierClient.ApiUrls {
        return CourierClient.ApiUrls(
            rest = restUrl ?: CourierClient.ApiUrls().rest,
            graphql = graphqlUrl ?: CourierClient.ApiUrls().graphql,
            inboxGraphql = inboxGraphqlUrl ?: CourierClient.ApiUrls().inboxGraphql,
            inboxWebSocket = inboxWebSocketUrl ?: CourierClient.ApiUrls().inboxWebSocket
        )
    }

    fun reset() {
        val defaultUrls = CourierClient.ApiUrls()
        prefs.edit {
            putString(KEY_USER_ID, null).apply()
            putString(KEY_TENANT_ID, null).apply()
            putString(KEY_API_KEY, Env.COURIER_AUTH_KEY).apply()
            putString(KEY_REST_URL, defaultUrls.rest).apply()
            putString(KEY_GRAPHQL_URL, defaultUrls.graphql).apply()
            putString(KEY_INBOX_GRAPHQL_URL, defaultUrls.inboxGraphql).apply()
            putString(KEY_INBOX_WEBSOCKET_URL, defaultUrls.inboxWebSocket).apply()
        }

    }

    companion object {
        private const val PREFS_NAME = "courier_auth_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_REST_URL = "rest_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_GRAPHQL_URL = "graphql_url"
        private const val KEY_INBOX_GRAPHQL_URL = "inbox_graphql_url"
        private const val KEY_INBOX_WEBSOCKET_URL = "inbox_websocket_url"
    }
}