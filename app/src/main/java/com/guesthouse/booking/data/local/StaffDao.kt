package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): StaffEntity?

    @Query("SELECT COUNT(*) FROM staff")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staff: List<StaffEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<StaffPropertyAssignmentEntity>)

    @Query("SELECT propertyId FROM staff_property_assignments WHERE staffId = :staffId")
    suspend fun assignedPropertyIds(staffId: Long): List<Long>
}
