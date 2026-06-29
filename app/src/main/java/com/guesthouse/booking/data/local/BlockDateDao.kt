package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDateDao {
    @Query("SELECT * FROM block_dates WHERE roomId = :roomId ORDER BY startEpochDay ASC")
    fun observeForRoom(roomId: Long): Flow<List<BlockDateEntity>>

    @Query("SELECT * FROM block_dates WHERE id = :id")
    suspend fun getById(id: Long): BlockDateEntity?

    @Query("""
        SELECT * FROM block_dates WHERE roomId = :roomId
        AND (:excludeId = 0 OR id != :excludeId)
        AND startEpochDay < :endEpochDay AND endEpochDay > :startEpochDay
    """)
    suspend fun findOverlapping(roomId: Long, startEpochDay: Long, endEpochDay: Long, excludeId: Long = 0L): List<BlockDateEntity>

    @Insert
    suspend fun insert(block: BlockDateEntity): Long

    @Query("DELETE FROM block_dates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
