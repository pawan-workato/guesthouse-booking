package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.RoomEntity
import kotlinx.coroutines.flow.Flow

class BookingRepository(private val database: AppDatabase) {
    fun observeRooms(): Flow<List<RoomEntity>> = database.roomDao().observeAll()

    fun observeRoom(roomId: Long): Flow<RoomEntity?> = database.roomDao().observeById(roomId)

    fun observeBookings(): Flow<List<BookingEntity>> = database.bookingDao().observeAll()

    fun observeConfirmedBookingsForRoom(roomId: Long): Flow<List<BookingEntity>> =
        database.bookingDao().observeConfirmedForRoom(roomId)

    suspend fun createBooking(
        roomId: Long,
        guestName: String,
        guestEmail: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long
    ): Result<Long> {
        if (guestName.isBlank() || guestEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Guest name and email are required"))
        }
        if (checkOutEpochDay <= checkInEpochDay) {
            return Result.failure(IllegalArgumentException("Check-out must be after check-in"))
        }
        val overlaps = database.bookingDao().findOverlapping(
            roomId, checkInEpochDay, checkOutEpochDay
        )
        if (overlaps.isNotEmpty()) {
            return Result.failure(IllegalStateException("Room is not available for those dates"))
        }
        val id = database.bookingDao().insert(
            BookingEntity(
                roomId = roomId,
                guestName = guestName.trim(),
                guestEmail = guestEmail.trim(),
                checkInEpochDay = checkInEpochDay,
                checkOutEpochDay = checkOutEpochDay
            )
        )
        return Result.success(id)
    }

    suspend fun cancelBooking(bookingId: Long) {
        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED)
    }
}
