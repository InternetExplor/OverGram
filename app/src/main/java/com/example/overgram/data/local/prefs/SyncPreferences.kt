package com.example.overgram.data.local.prefs

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * The update-stream cursor (last applied `updateSeq`). Lives in the auth prefs file on purpose:
 * logging out clears it together with the session, so the next account starts fresh.
 */
class SyncPreferences(
    private val prefs: SharedPreferences
) {

    /** Null until bootstrapped from `GET /v1/updates/state`. */
    fun getCursor(): Long? = if (prefs.contains(KEY_CURSOR)) prefs.getLong(KEY_CURSOR, 0L) else null

    fun setCursor(updateSeq: Long) {
        prefs.edit { putLong(KEY_CURSOR, updateSeq) }
    }

    private companion object {
        const val KEY_CURSOR = "update_cursor"
    }
}
