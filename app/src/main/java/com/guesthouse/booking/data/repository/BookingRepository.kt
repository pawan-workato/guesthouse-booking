package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

data class BookingCreateOutcome(
    val bookingId: Long,
    val reference: String,
    val savedOffline: Boolean
)

class BookingRepository(private val database: AppDatabase) {
    fun observeProperties(): Flow<List<PropertyEntity>> = database.propertyDao().observeAll()
    fun observeProperty(propertyId: Long): Flow<PropertyEntity?> = database.propertyDao().observeById(propertyId)
    fun observeRooms(): Flow<List<RoomEntity>> = database.roomDao().observeAll()
    fun observeRoomsForProperty(propertyId: Long): Flow<List<RoomEntity>> = database.roomDao().observeByPropertyId(propertyId)
    fun observeRoom(roomId: Long): Flow<RoomEntity?> = database.roomDao().observeById(roomId)
    suspend fun getRoomById(roomId: Long): RoomEntity? = database.roomDao().getById(roomId)
    fun observeBookings(): Flow<List<BookingEntity>> = database.bookingDao().observeAll()
    fun observeActiveBookingsForRoom(roomId: Long): Flow<List<BookingEntity>> = database.bookingDao().observeActiveForRoom(roomId)

    suspend fun createBooking(
        roomId: Long,
        guestId: Long?,
        guestName: String,
        guestEmail: String,
        guestPhone: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long,
        isOnline: Boolean
    ): Result<BookingCreateOutcome> {
        if (guestName.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        if (checkOutEpochDay <= checkInEpochDay) return Result.failure(IllegalArgumentException("Check-out must be after check-in"))
        val room = database.roomDao().getById(roomId) ?: return Result.failure(IllegalArgumentException("Room not found"))
        if (database.bookingDao().findOverlapping(roomId, checkInEpochDay, checkOutEpochDay).isNotEmpty()) {
            return Result.failure(IllegalStateException("Room is not available for those dates"))
        }
        val id = database.bookingDao().insert(
            BookingEntity(
                propertyId = room.propertyId,
                roomId = roomId,
                guestId = guestId,
                guestName = guestName.trim(),
                guestEmail = guestEmail.trim(),
                guestPhone = guestPhone.trim(),
                checkInEpochDay = checkInEpochDay,
                checkOutEpochDay = checkOutEpochDay,
                syncStatus = if (isOnline) SyncStatus.SYNCED.name else SyncStatus.PENDING_SYNC.name
            )
        )
        val reference = if (isOnline) {
            SyncRepository.formatReference(room.propertyId, id).also {
                database.bookingDao().updateSync(id, SyncStatus.SYNCED.name, it)
            }
        } else {
            SyncRepository.formatOfflineReference(id).also {
                database.bookingDao().updateSync(id, SyncStatus.PENDING_SYNC.name, it)
            }
        }
        return Result.success(BookingCreateOutcome(id, reference, savedOffline = !isOnline))
    }

    suspend fun cancelBooking(bookingId: Long) {
        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED.name)
    }
}
