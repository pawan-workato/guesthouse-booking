package com.guesthouse.booking.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/** Stores the Room SQLCipher passphrase in EncryptedSharedPreferences (Android Keystore-backed). */
internal object DatabaseKeyManager {
    private const val PREFS_NAME = "guesthouse_db_key"
    private const val KEY_PASSPHRASE = "passphrase_b64"
    private const val PASSPHRASE_BYTES = 32

    fun getPassphrase(context: Context): ByteArray {
        val appContext = context.applicationContext
        val prefs = EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString(KEY_PASSPHRASE, null)?.let { stored ->
            return Base64.decode(stored, Base64.NO_WRAP)
        }
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
            .apply()
        return passphrase
    }
}
