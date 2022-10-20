package com.courier.android.managers

import android.content.Context
import android.content.SharedPreferences
import com.courier.android.Courier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UserManager {

    companion object {

        private const val ACCESS_TOKEN = "courier_access_token"
        private const val USER_ID = "courier_user_id"

        private val Context.sharedPrefs get(): SharedPreferences {
            return getSharedPreferences(Courier.TAG, Context.MODE_PRIVATE)
        }

        suspend fun setCredentials(context: Context, accessToken: String, userId: String) = withContext(Dispatchers.IO) {
            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, accessToken)
            prefs.putString(USER_ID, userId)
            prefs.commit()
        }

        fun getAccessToken(context: Context): String? {
            return context.sharedPrefs.getString(ACCESS_TOKEN, null)
        }

        fun getUserId(context: Context): String? {
            return context.sharedPrefs.getString(USER_ID, null)
        }

        suspend fun removeCredentials(context: Context) = withContext(Dispatchers.IO) {
            val prefs = context.sharedPrefs.edit()
            prefs.putString(ACCESS_TOKEN, null)
            prefs.putString(USER_ID, null)
            prefs.commit()
        }

    }

}