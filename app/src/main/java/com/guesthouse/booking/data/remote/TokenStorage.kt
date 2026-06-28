package com.guesthouse.booking.data.remote
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
class TokenStorage(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(context, PREFS, MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(), EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    fun saveSession(token: String, staff: StaffDto) { prefs.edit().putString(K_TOKEN, token).putLong(K_ID, staff.id).putString(K_EMAIL, staff.email).putString(K_NAME, staff.displayName).putString(K_ROLE, staff.role).putString(K_ASSIGNED, staff.assignedPropertyIds.joinToString(",")).apply() }
    fun getToken(): String? = prefs.getString(K_TOKEN, null)
    fun hasToken(): Boolean = !getToken().isNullOrBlank()
    fun loadStaff(): StaffDto? {
        val id = prefs.getLong(K_ID, -1); if (id <= 0) return null
        val email = prefs.getString(K_EMAIL, null) ?: return null
        val name = prefs.getString(K_NAME, null) ?: return null
        val role = prefs.getString(K_ROLE, null) ?: return null
        val assigned = prefs.getString(K_ASSIGNED, "")?.split(",")?.filter { it.isNotBlank() }?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        return StaffDto(id, email, name, role, assigned)
    }
    fun clear() { prefs.edit().clear().apply() }
    companion object { private const val PREFS = "guesthouse_ktor_auth"; private const val K_TOKEN = "token"; private const val K_ID = "staff_id"; private const val K_EMAIL = "email"; private const val K_NAME = "display_name"; private const val K_ROLE = "role"; private const val K_ASSIGNED = "assigned" }
}
