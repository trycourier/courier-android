package com.courier.android.managers

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class UserManager {

    companion object {

        private const val TAG = "Courier SDK"
        private const val USER_ID = "courier_user_id"
        private const val ACCESS_TOKEN = "courier_access_token"
        private const val CLIENT_KEY = "courier_client_key"
        private const val TENANT_ID = "courier_tenant_id"

        private val Context.sharedPrefs get(): SharedPreferences {
            return getSharedPreferences(TAG, Context.MODE_PRIVATE)
        }

        suspend fun setCredentials(context: Context, userId: String, accessToken: String, clientKey: String?, tenantId: String?) = withContext(Dispatchers.IO) {
            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, accessToken)
            prefs.putString(USER_ID, userId)
            prefs.putString(CLIENT_KEY, clientKey)
            prefs.putString(TENANT_ID, tenantId)
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

        suspend fun removeCredentials(context: Context) = withContext(Dispatchers.IO) {
            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, null)
            prefs.putString(USER_ID, null)
            prefs.putString(CLIENT_KEY, null)
            prefs.putString(TENANT_ID, null)
            return@withContext prefs.commit()
        }

    }

}