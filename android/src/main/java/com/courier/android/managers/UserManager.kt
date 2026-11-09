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

        // The access token and client key are encrypted with a keystore key, and everything else
        // stays in plain preferences. Replaceable so tests can swap the store.
        @Volatile
        internal var secretStore: SecretStore? = null

        private fun secrets(context: Context): SecretStore {
            return secretStore ?: synchronized(this) {
                secretStore ?: KeystoreSecretStore(context).also { secretStore = it }
            }
        }

        // Reads a secret, moving one saved in plain preferences by an earlier version into the
        // keystore. If the keystore can't be used, the plain value is kept and used.
        private fun secret(context: Context, key: String): String? {
            val prefs = context.sharedPrefs
            val plain = prefs.getString(key, null)
            return when (val read = secrets(context).read(key)) {
                is SecretRead.Found -> read.value
                SecretRead.Unavailable -> plain
                SecretRead.Missing -> {
                    if (plain != null && secrets(context).write(key, plain)) {
                        prefs.edit().remove(key).apply()
                    }
                    plain
                }
            }
        }

        suspend fun setCredentials(context: Context, userId: String, accessToken: String, clientKey: String?, tenantId: String?, apiUrls: CourierClient.ApiUrls, showLogs: Boolean) = withContext(Dispatchers.IO) {
            // The secrets go to the keystore. If it can't be written, they stay in plain
            // preferences as before, so signing in still works.
            val secrets = secrets(context)
            val tokenSaved = secrets.write(ACCESS_TOKEN, accessToken)
            secrets.delete(CLIENT_KEY)
            val keySaved = clientKey == null || secrets.write(CLIENT_KEY, clientKey)

            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, if (tokenSaved) null else accessToken)
            prefs.putString(USER_ID, userId)
            prefs.putString(CLIENT_KEY, if (keySaved) null else clientKey)
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
            return secret(context, ACCESS_TOKEN)
        }

        fun getClientKey(context: Context): String? {
            return secret(context, CLIENT_KEY)
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
            secrets(context).delete(ACCESS_TOKEN)
            secrets(context).delete(CLIENT_KEY)
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