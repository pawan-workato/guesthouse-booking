package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guesthouse.booking.data.local.entities.GuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestDao {
    @Query("SELECT * FROM guests WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<GuestEntity>>

    @Query("SELECT * FROM guests ORDER BY name ASC")
    fun observeAllIncludingInactive(): Flow<List<GuestEntity>>

    @Query("SELECT * FROM guests WHERE id = :id")
    fun observeById(id: Long): Flow<GuestEntity?>

    @Query("SELECT * FROM guests WHERE id = :id")
    suspend fun getById(id: Long): GuestEntity?

    @Query("SELECT COUNT(*) FROM guests")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guests: List<GuestEntity>)

    @Insert
    suspend fun insert(guest: GuestEntity): Long

    @Update
    suspend fun update(guest: GuestEntity)

    @Query("UPDATE guests SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
