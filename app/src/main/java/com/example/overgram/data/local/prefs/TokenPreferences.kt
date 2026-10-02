package com.example.overgram.data.local.prefs

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists the auth session in the "auth_prefs" SharedPreferences file.
 */
class TokenPreferences(
    private val prefs: SharedPreferences
) {

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    fun hasSession(): Boolean = !getRefreshToken().isNullOrEmpty()

    fun save(accessToken: String, refreshToken: String, userId: String, deviceId: String) {
        // commit() so the session is on disk before the auth flow navigates away.
        prefs.edit(commit = true) {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putString(KEY_DEVICE_ID, deviceId)
        }
    }

    fun clear() {
        prefs.edit(commit = true) { clear() }
    }

    companion object {
        const val PREFS_NAME = "auth_prefs"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
