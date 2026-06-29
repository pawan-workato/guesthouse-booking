package com.guesthouse.booking.data.auth

import android.content.Context

/** Removes legacy session SharedPreferences that stored `staff_id` (KR-02). Session identity is Firebase Auth UID only. */
internal object LegacySessionStorage {
    private val LEGACY_PREF_NAMES = listOf(
        "guesthouse_auth",
        "guesthouse_auth_secure"
    )

    fun purge(context: Context) {
        val appContext = context.applicationContext
        LEGACY_PREF_NAMES.forEach { name ->
            runCatching { appContext.deleteSharedPreferences(name) }
        }
    }
}
