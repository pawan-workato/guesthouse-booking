package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class StaffProfileDetails(
    val staffId: Long,
    val email: String,
    val displayName: String,
    val role: StaffRole,
    val roleLabel: String,
    val assignedPropertyNames: List<String>,
    val isChainAdmin: Boolean
)

class ProfileRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val propertyRepository: PropertyRepository,
    private val firestore: FirestoreDataSource,
    private val networkMonitor: NetworkMonitor
) {
    fun observeProfile(): Flow<StaffProfileDetails?> = combine(
        authRepository.session,
        propertyRepository.observeAllProperties()
    ) { session, properties ->
        if (session == null) return@combine null
        val propertyNames = if (session.isChainAdmin) {
            listOf("All properties")
        } else {
            properties.filter { it.id in session.assignedPropertyIds }.map { it.name }.sorted()
        }
        StaffProfileDetails(
            staffId = session.staffId,
            email = session.email,
            displayName = session.displayName,
            role = session.role,
            roleLabel = roleLabel(session.role),
            assignedPropertyNames = propertyNames,
            isChainAdmin = session.isChainAdmin
        )
    }

    suspend fun updateDisplayName(displayName: String): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val staff = database.staffDao().findById(session.staffId)
            ?: return Result.failure(IllegalArgumentException("Staff profile not found"))
        if (staff.id != session.staffId) {
            return Result.failure(IllegalStateException("Cannot edit another staff profile"))
        }
        val trimmed = displayName.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Display name is required"))
        if (trimmed == staff.displayName) return Result.success(Unit)

        val previousRole = staff.role
        database.staffDao().update(staff.copy(displayName = trimmed))
        if (database.staffDao().findById(session.staffId)?.role != previousRole) {
            database.staffDao().update(staff)
            return Result.failure(IllegalStateException("Profile update rejected"))
        }

        authRepository.patchSessionDisplayName(trimmed)
        val uid = authRepository.currentFirebaseUid()
        if (uid != null && networkMonitor.isCurrentlyOnline()) {
            runCatching { firestore.updateStaffDisplayName(uid, trimmed) }.onFailure { error ->
                database.staffDao().update(staff)
                authRepository.patchSessionDisplayName(staff.displayName)
                return Result.failure(error)
            }
        }
        return Result.success(Unit)
    }

    private fun roleLabel(role: StaffRole): String = when (role) {
        StaffRole.CHAIN_ADMIN -> "Chain admin"
        StaffRole.PROPERTY_MANAGER -> "Property manager"
    }
}
