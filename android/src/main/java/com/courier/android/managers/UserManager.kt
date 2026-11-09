package com.courier.android.managers

import android.content.Context
import android.content.SharedPreferences
import com.courier.android.client.CourierClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class UserManager {

    companion object {

        private const val TAG = "Courier SDK"
        private const val USER_ID = "courier_user_id"
        private const val ACCESS_TOKEN = "courier_access_token"
        private const val CLIENT_KEY = "courier_client_key"
        private const val TENANT_ID = "courier_tenant_id"
        private const val REST_URL = "courier_rest_url"
        private const val GRAPHQL_URL = "courier_graphql_url"
        private const val INBOX_GRAPHQL_URL = "courier_inbox_graphql_url"
        private const val INBOX_WEBSOCKET_URL = "courier_inbox_websocket_url"
        private const val SHOW_LOGS = "courier_show_logs"

        private val Context.sharedPrefs get(): SharedPreferences {
            return getSharedPreferences(TAG, Context.MODE_PRIVATE)
        }

        suspend fun setCredentials(context: Context, userId: String, accessToken: String, clientKey: String?, tenantId: String?, apiUrls: CourierClient.ApiUrls, showLogs: Boolean) = withContext(Dispatchers.IO) {
            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, accessToken)
            prefs.putString(USER_ID, userId)
            prefs.putString(CLIENT_KEY, clientKey)
            prefs.putString(TENANT_ID, tenantId)
            prefs.putString(REST_URL, apiUrls.rest)
            prefs.putString(GRAPHQL_URL, apiUrls.graphql)
            prefs.putString(INBOX_GRAPHQL_URL, apiUrls.inboxGraphql)
            prefs.putString(INBOX_WEBSOCKET_URL, apiUrls.inboxWebSocket)
            prefs.putBoolean(SHOW_LOGS, showLogs)
            return@withContext prefs.commit()
        }

        fun getUserId(context: Context): String? {
            return context.sharedPrefs.getString(USER_ID, null)
        }

        fun getAccessToken(context: Context): String? {
            return context.sharedPrefs.getString(ACCESS_TOKEN, null)
        }

        fun getClientKey(context: Context): String? {
            return context.sharedPrefs.getString(CLIENT_KEY, null)
        }

        fun getTenantId(context: Context): String? {
            return context.sharedPrefs.getString(TENANT_ID, null)
        }

        // Null for sessions saved before the urls were stored
        fun getApiUrls(context: Context): CourierClient.ApiUrls? {
            val prefs = context.sharedPrefs
            return CourierClient.ApiUrls(
                rest = prefs.getString(REST_URL, null) ?: return null,
                graphql = prefs.getString(GRAPHQL_URL, null) ?: return null,
                inboxGraphql = prefs.getString(INBOX_GRAPHQL_URL, null) ?: return null,
                inboxWebSocket = prefs.getString(INBOX_WEBSOCKET_URL, null) ?: return null,
            )
        }

        fun getShowLogs(context: Context): Boolean? {
            val prefs = context.sharedPrefs
            return if (prefs.contains(SHOW_LOGS)) prefs.getBoolean(SHOW_LOGS, false) else null
        }

        suspend fun removeCredentials(context: Context) = withContext(Dispatchers.IO) {
            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, null)
            prefs.putString(USER_ID, null)
            prefs.putString(CLIENT_KEY, null)
            prefs.putString(TENANT_ID, null)
            prefs.remove(REST_URL)
            prefs.remove(GRAPHQL_URL)
            prefs.remove(INBOX_GRAPHQL_URL)
            prefs.remove(INBOX_WEBSOCKET_URL)
            prefs.remove(SHOW_LOGS)
            return@withContext prefs.commit()
        }

    }

}