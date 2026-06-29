package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guesthouse.booking.data.local.entities.RoomEntity
import kotlinx.coroutines.flow.Flow

data class RoomTypeCountRow(val roomType: String, val count: Int)

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY name ASC")
    fun observeAll(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms ORDER BY name ASC")
    suspend fun getAll(): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE propertyId = :propertyId ORDER BY name ASC")
    fun observeByPropertyId(propertyId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    fun observeById(id: Long): Flow<RoomEntity?>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getById(id: Long): RoomEntity?

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun count(): Int

    @Query(
        """
        SELECT roomType, COUNT(*) AS count FROM rooms
        WHERE propertyId = :propertyId
        GROUP BY roomType
        ORDER BY roomType ASC
        """
    )
    suspend fun countByTypeForProperty(propertyId: Long): List<RoomTypeCountRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<RoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: RoomEntity): Long

    @Update
    suspend fun update(room: RoomEntity)

    @Query("SELECT COUNT(*) FROM rooms WHERE propertyId = :propertyId")
    suspend fun countForProperty(propertyId: Long): Int

}
