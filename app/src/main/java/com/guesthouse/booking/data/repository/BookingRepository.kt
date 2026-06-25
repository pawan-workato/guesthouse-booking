package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import kotlinx.coroutines.flow.Flow

class BookingRepository(private val database: AppDatabase) {
    fun observeProperties(): Flow<List<PropertyEntity>> = database.propertyDao().observeAll()

    fun observeProperty(propertyId: Long): Flow<PropertyEntity?> =
        database.propertyDao().observeById(propertyId)

    fun observeRooms(): Flow<List<RoomEntity>> = database.roomDao().observeAll()

    fun observeRoomsForProperty(propertyId: Long): Flow<List<RoomEntity>> =
        database.roomDao().observeByPropertyId(propertyId)

    fun observeRoom(roomId: Long): Flow<RoomEntity?> = database.roomDao().observeById(roomId)

    suspend fun getRoomById(roomId: Long): RoomEntity? = database.roomDao().getById(roomId)

    fun observeBookings(): Flow<List<BookingEntity>> = database.bookingDao().observeAll()

    fun observeConfirmedBookingsForRoom(roomId: Long): Flow<List<BookingEntity>> =
        database.bookingDao().observeConfirmedForRoom(roomId, BookingStatus.CONFIRMED.name)

    suspend fun createBooking(
        roomId: Long,
        guestName: String,
        guestEmail: String,
        guestPhone: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long
    ): Result<Long> {
        if (guestName.isBlank()) {
            return Result.failure(IllegalArgumentException("Guest name is required"))
        }
        if (checkOutEpochDay <= checkInEpochDay) {
            return Result.failure(IllegalArgumentException("Check-out must be after check-in"))
        }
        val room = database.roomDao().getById(roomId)
            ?: return Result.failure(IllegalArgumentException("Room not found"))
        val overlaps = database.bookingDao().findOverlapping(
            roomId, checkInEpochDay, checkOutEpochDay, BookingStatus.CONFIRMED.name
        )
        if (overlaps.isNotEmpty()) {
            return Result.failure(IllegalStateException("Room is not available for those dates"))
        }
        val id = database.bookingDao().insert(
            BookingEntity(
                propertyId = room.propertyId,
                roomId = roomId,
                guestName = guestName.trim(),
                guestEmail = guestEmail.trim(),
                guestPhone = guestPhone.trim(),
                checkInEpochDay = checkInEpochDay,
                checkOutEpochDay = checkOutEpochDay
            )
        )
        return Result.success(id)
    }

    suspend fun cancelBooking(bookingId: Long) {
        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED.name)
    }
}
