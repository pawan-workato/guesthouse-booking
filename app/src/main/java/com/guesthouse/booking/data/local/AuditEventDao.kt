package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.guesthouse.booking.data.local.entities.AuditEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditEventDao {

    @Query("SELECT * FROM audit_events ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<AuditEventEntity>>

    @Query(
        """
        SELECT * FROM audit_events
        WHERE propertyId IS NULL OR propertyId IN (:propertyIds)
        ORDER BY createdAtEpochMs DESC
        """
    )
    fun observeForProperties(propertyIds: List<Long>): Flow<List<AuditEventEntity>>

    @Insert
    suspend fun insert(event: AuditEventEntity): Long
}
