package com.guesthouse.booking.data.repository

import androidx.room.withTransaction
import com.guesthouse.booking.data.auth.PasswordHasher
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class StaffWithAssignments(
    val staff: StaffEntity,
    val assignedPropertyIds: Set<Long>
)

class StaffRepository(private val database: AppDatabase) {
    fun observeStaff(includeInactive: Boolean): Flow<List<StaffWithAssignments>> {
        val staffFlow = if (includeInactive) {
            database.staffDao().observeAllIncludingInactive()
        } else {
            database.staffDao().observeActive()
        }
        return combine(staffFlow, database.staffDao().observeAllAssignments()) { staffList, assignments ->
            val byStaff = assignments.groupBy { it.staffId }.mapValues { (_, rows) -> rows.map { it.propertyId }.toSet() }
            staffList.map { staff ->
                val assigned = if (staff.role == StaffRole.CHAIN_ADMIN.name) {
                    emptySet()
                } else {
                    byStaff[staff.id] ?: emptySet()
                }
                StaffWithAssignments(staff = staff, assignedPropertyIds = assigned)
            }
        }
    }

    suspend fun getStaff(staffId: Long): StaffWithAssignments? {
        val staff = database.staffDao().findById(staffId) ?: return null
        return StaffWithAssignments(staff, loadAssignments(staff))
    }

    suspend fun createManager(
        email: String,
        displayName: String,
        password: String,
        propertyIds: Set<Long>
    ): Result<Long> {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Display name is required"))
        if (trimmedEmail.isBlank()) return Result.failure(IllegalArgumentException("Email is required"))
        if (password.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        if (propertyIds.isEmpty()) return Result.failure(IllegalArgumentException("Select at least one property"))
        if (database.staffDao().findByEmail(trimmedEmail) != null) {
            return Result.failure(IllegalArgumentException("Email is already in use"))
        }
        val id = database.withTransaction {
            val staffId = database.staffDao().insert(
                StaffEntity(
                    email = trimmedEmail,
                    passwordHash = PasswordHasher.hash(password),
                    displayName = trimmedName,
                    role = StaffRole.PROPERTY_MANAGER.name,
                    isActive = true
                )
            )
            database.staffDao().insertAssignments(
                propertyIds.map { StaffPropertyAssignmentEntity(staffId, it) }
            )
            staffId
        }
        return Result.success(id)
    }

    suspend fun updateStaff(
        staffId: Long,
        email: String,
        displayName: String,
        propertyIds: Set<Long>
    ): Result<Unit> {
        val existing = database.staffDao().findById(staffId)
            ?: return Result.failure(IllegalArgumentException("Staff member not found"))
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Display name is required"))
        if (trimmedEmail.isBlank()) return Result.failure(IllegalArgumentException("Email is required"))
        if (database.staffDao().findByEmailExcludingId(trimmedEmail, staffId) != null) {
            return Result.failure(IllegalArgumentException("Email is already in use"))
        }
        val role = runCatching { StaffRole.valueOf(existing.role) }.getOrNull()
            ?: return Result.failure(IllegalStateException("Invalid staff role"))
        if (role == StaffRole.PROPERTY_MANAGER && existing.isActive && propertyIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Managers must have at least one property"))
        }
        database.withTransaction {
            database.staffDao().update(
                existing.copy(email = trimmedEmail, displayName = trimmedName)
            )
            if (role == StaffRole.PROPERTY_MANAGER) {
                database.staffDao().deleteAssignmentsForStaff(staffId)
                if (propertyIds.isNotEmpty()) {
                    database.staffDao().insertAssignments(
                        propertyIds.map { StaffPropertyAssignmentEntity(staffId, it) }
                    )
                }
            }
        }
        return Result.success(Unit)
    }

    suspend fun setStaffActive(staffId: Long, active: Boolean): Result<Unit> {
        val staff = database.staffDao().findById(staffId)
            ?: return Result.failure(IllegalArgumentException("Staff member not found"))
        if (!active && staff.role == StaffRole.CHAIN_ADMIN.name) {
            val adminCount = database.staffDao().countActiveByRole(StaffRole.CHAIN_ADMIN.name)
            if (adminCount <= 1) {
                return Result.failure(IllegalArgumentException("Cannot remove the last chain admin"))
            }
        }
        database.staffDao().setActive(staffId, active)
        return Result.success(Unit)
    }

    private suspend fun loadAssignments(staff: StaffEntity): Set<Long> {
        if (staff.role == StaffRole.CHAIN_ADMIN.name) return emptySet()
        return database.staffDao().assignedPropertyIds(staff.id).toSet()
    }
}
