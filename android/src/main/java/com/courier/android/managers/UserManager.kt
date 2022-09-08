package com.courier.android.managers

import android.content.Context
import android.content.SharedPreferences
import com.courier.android.Courier

sealed class UserManager {

    companion object {

        private const val ACCESS_TOKEN = "courier_access_token"
        private const val USER_ID = "courier_user_id"

        private val Context.sharedPrefs get(): SharedPreferences {
            return getSharedPreferences(Courier.TAG, Context.MODE_PRIVATE)
        }

        fun setCredentials(context: Context, accessToken: String, userId: String) {
            with (context.sharedPrefs.edit()) {
                putString(ACCESS_TOKEN, accessToken)
                putString(USER_ID, userId)
                apply()
            }
        }

        fun getAccessToken(context: Context): String? {
            return context.sharedPrefs.getString(ACCESS_TOKEN, null)
        }

        fun getUserId(context: Context): String? {
            return context.sharedPrefs.getString(USER_ID, null)
        }

        fun removeCredentials(context: Context) {
            with (context.sharedPrefs.edit()) {
                putString(ACCESS_TOKEN, null)
                putString(USER_ID, null)
                apply()
            }
        }

    }

}