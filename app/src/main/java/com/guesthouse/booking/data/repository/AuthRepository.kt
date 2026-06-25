package com.guesthouse.booking.data.repository

import android.content.Context
import com.guesthouse.booking.data.auth.PasswordHasher
import com.guesthouse.booking.data.auth.StaffSession
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.StaffRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val database: AppDatabase,
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _session = MutableStateFlow<StaffSession?>(null)
    val session: StateFlow<StaffSession?> = _session.asStateFlow()

    suspend fun restoreSession() {
        val staffId = prefs.getLong(KEY_STAFF_ID, -1L)
        if (staffId <= 0L) return
        _session.value = loadSession(staffId) ?: run {
            clearPersistedSession()
            null
        }
    }

    suspend fun login(email: String, password: String): Result<StaffSession> {
        val staff = database.staffDao().findByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("Invalid email or password"))
        if (!PasswordHasher.verify(password, staff.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid email or password"))
        }
        val session = buildSession(staff.id, staff.email, staff.displayName, staff.role)
            ?: return Result.failure(IllegalStateException("Staff account is misconfigured"))
        persistSession(session.staffId)
        _session.value = session
        return Result.success(session)
    }

    fun logout() {
        clearPersistedSession()
        _session.value = null
    }

    fun currentSession(): StaffSession? = _session.value

    private suspend fun loadSession(staffId: Long): StaffSession? {
        val staff = database.staffDao().findById(staffId) ?: return null
        return buildSession(staff.id, staff.email, staff.displayName, staff.role)
    }

    private suspend fun buildSession(
        staffId: Long,
        email: String,
        displayName: String,
        roleName: String
    ): StaffSession? {
        val role = runCatching { StaffRole.valueOf(roleName) }.getOrNull() ?: return null
        val assigned = if (role == StaffRole.CHAIN_ADMIN) {
            emptySet()
        } else {
            database.staffDao().assignedPropertyIds(staffId).toSet()
        }
        return StaffSession(
            staffId = staffId,
            email = email,
            displayName = displayName,
            role = role,
            assignedPropertyIds = assigned
        )
    }

    private fun persistSession(staffId: Long) {
        prefs.edit().putLong(KEY_STAFF_ID, staffId).apply()
    }

    private fun clearPersistedSession() {
        prefs.edit().remove(KEY_STAFF_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "guesthouse_auth"
        private const val KEY_STAFF_ID = "staff_id"
    }
}
