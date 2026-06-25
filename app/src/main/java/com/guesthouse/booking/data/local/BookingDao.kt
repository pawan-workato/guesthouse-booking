package com.guesthouse.booking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY checkInEpochDay DESC")
    fun observeAll(): Flow<List<BookingEntity>>

    @Query(
        """
        SELECT * FROM bookings
        WHERE roomId = :roomId AND status = 'CONFIRMED'
        AND checkInEpochDay < :checkOut AND checkOutEpochDay > :checkIn
        """
    )
    suspend fun findOverlapping(
        roomId: Long,
        checkIn: Long,
        checkOut: Long
    ): List<BookingEntity>

    @Insert
    suspend fun insert(booking: BookingEntity): Long

    @Update
    suspend fun update(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: BookingStatus)
}
