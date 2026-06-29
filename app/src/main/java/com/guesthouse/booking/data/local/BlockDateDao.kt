package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDateDao {
    @Query("SELECT * FROM block_dates WHERE roomId = :roomId AND markedForDeletion = 0 ORDER BY startEpochDay ASC")
    fun observeForRoom(roomId: Long): Flow<List<BlockDateEntity>>

    @Query("SELECT * FROM block_dates WHERE id = :id")
    suspend fun getById(id: Long): BlockDateEntity?

    @Query("""
        SELECT * FROM block_dates WHERE roomId = :roomId
        AND markedForDeletion = 0
        AND syncStatus != :conflictStatus
        AND (:excludeId = 0 OR id != :excludeId)
        AND startEpochDay < :endEpochDay AND endEpochDay > :startEpochDay
    """)
    suspend fun findOverlapping(
        roomId: Long,
        startEpochDay: Long,
        endEpochDay: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name,
        excludeId: Long = 0L
    ): List<BlockDateEntity>

    @Insert
    suspend fun insert(block: BlockDateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(blocks: List<BlockDateEntity>)

    @Query("SELECT * FROM block_dates WHERE syncStatus = :syncStatus ORDER BY createdAtEpochMs ASC")
    suspend fun getBySyncStatus(syncStatus: String): List<BlockDateEntity>

    @Query("SELECT * FROM block_dates WHERE syncStatus = :syncStatus AND markedForDeletion = 0 ORDER BY createdAtEpochMs ASC")
    fun observeBySyncStatus(syncStatus: String): Flow<List<BlockDateEntity>>

    @Query("SELECT COUNT(*) FROM block_dates WHERE syncStatus = :syncStatus AND markedForDeletion = 0")
    fun observeCountBySyncStatus(syncStatus: String): Flow<Int>

    @Query("UPDATE block_dates SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)

    @Query("UPDATE block_dates SET markedForDeletion = 1, syncStatus = :syncStatus WHERE id = :id")
    suspend fun markForDeletion(id: Long, syncStatus: String)

    @Query("DELETE FROM block_dates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        SELECT DISTINCT roomId FROM block_dates
        WHERE roomId IN (SELECT id FROM rooms WHERE propertyId = :propertyId)
        AND markedForDeletion = 0
        AND syncStatus != :conflictStatus
        AND startEpochDay <= :epochDay AND endEpochDay > :epochDay
        """
    )
    suspend fun getBlockedRoomIdsForProperty(
        propertyId: Long,
        epochDay: Long,
        conflictStatus: String = SyncStatus.CONFLICT.name
    ): List<Long>

}
