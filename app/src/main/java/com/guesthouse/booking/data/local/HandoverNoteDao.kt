package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.guesthouse.booking.data.local.entities.HandoverNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HandoverNoteDao {
    @Query("SELECT * FROM handover_notes ORDER BY createdAtEpochMs DESC LIMIT 200")
    fun observeAll(): Flow<List<HandoverNoteEntity>>

    @Query("SELECT * FROM handover_notes WHERE propertyId = :propertyId ORDER BY createdAtEpochMs DESC LIMIT 200")
    fun observeForProperty(propertyId: Long): Flow<List<HandoverNoteEntity>>

    @Insert
    suspend fun insert(note: HandoverNoteEntity): Long
}
