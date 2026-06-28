package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE email = :email COLLATE NOCASE AND id != :excludeId LIMIT 1")
    suspend fun findByEmailExcludingId(email: String, excludeId: Long): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): StaffEntity?

    @Query("SELECT * FROM staff WHERE firebaseUid = :uid LIMIT 1")
    suspend fun findByFirebaseUid(uid: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE isActive = 1 ORDER BY displayName ASC")
    fun observeActive(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff ORDER BY displayName ASC")
    fun observeAllIncludingInactive(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff ORDER BY displayName ASC")
    suspend fun getAll(): List<StaffEntity>

    @Query("SELECT COUNT(*) FROM staff")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM staff WHERE role = :role AND isActive = 1")
    suspend fun countActiveByRole(role: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staff: List<StaffEntity>)

    @Insert
    suspend fun insert(staff: StaffEntity): Long

    @Update
    suspend fun update(staff: StaffEntity)

    @Query("UPDATE staff SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE staff SET firebaseUid = :uid WHERE id = :id")
    suspend fun updateFirebaseUid(id: Long, uid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<StaffPropertyAssignmentEntity>)

    @Query("DELETE FROM staff_property_assignments WHERE staffId = :staffId")
    suspend fun deleteAssignmentsForStaff(staffId: Long)

    @Query("SELECT propertyId FROM staff_property_assignments WHERE staffId = :staffId")
    suspend fun assignedPropertyIds(staffId: Long): List<Long>

    @Query("SELECT * FROM staff_property_assignments")
    fun observeAllAssignments(): Flow<List<StaffPropertyAssignmentEntity>>
}
