package com.guesthouse.booking.data.repository

import com.guesthouse.booking.BuildConfig
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class BookingCreateOutcome(val bookingId: Long, val reference: String, val savedOffline: Boolean)

class BookingRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val firestore: FirestoreDataSource = FirestoreDataSource(),
    private val syncRepository: Lazy<SyncRepository> = lazy { error("SyncRepository not initialized") }
) {
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

        val useFirestore = !BuildConfig.USE_KTOR_API &&
            isOnline && networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
        val syncStatus = if (useFirestore) SyncStatus.SYNCED.name else SyncStatus.PENDING_SYNC.name
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
                syncStatus = syncStatus
            )
        )

        if (useFirestore) {
            val reference = SyncRepository.formatReference(room.propertyId, id)
            database.bookingDao().updateSync(id, SyncStatus.SYNCED.name, reference)
            database.bookingDao().getById(id)?.let { runCatching { firestore.upsertBooking(it) } }
            return Result.success(BookingCreateOutcome(id, reference, savedOffline = false))
        }

        val offlineRef = SyncRepository.formatOfflineReference(id)
        database.bookingDao().updateSync(id, SyncStatus.PENDING_SYNC.name, offlineRef)
        if (isOnline) runCatching { syncRepository.value.syncNow() }
        else syncRepository.value.enqueueSyncWorker()
        return Result.success(BookingCreateOutcome(id, offlineRef, savedOffline = true))
    }

    suspend fun getBookingById(bookingId: Long): BookingEntity? = database.bookingDao().getById(bookingId)

    suspend fun updateBooking(
        bookingId: Long,
        roomId: Long,
        guestId: Long?,
        guestName: String,
        guestEmail: String,
        guestPhone: String,
        checkInEpochDay: Long,
        checkOutEpochDay: Long,
        isOnline: Boolean
    ): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = database.bookingDao().getById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Booking not found"))
        if (!session.canAccessProperty(existing.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (existing.status != BookingStatus.CONFIRMED.name) {
            return Result.failure(IllegalStateException("Only confirmed bookings can be edited"))
        }
        if (guestName.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        if (checkOutEpochDay <= checkInEpochDay) {
            return Result.failure(IllegalArgumentException("Check-out must be after check-in"))
        }
        val room = database.roomDao().getById(roomId) ?: return Result.failure(IllegalArgumentException("Room not found"))
        if (!session.canAccessProperty(room.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (database.bookingDao().findOverlapping(roomId, checkInEpochDay, checkOutEpochDay, excludeId = bookingId).isNotEmpty()) {
            return Result.failure(IllegalStateException("Room is not available for those dates"))
        }

        val updated = existing.copy(
            propertyId = room.propertyId,
            roomId = roomId,
            guestId = guestId,
            guestName = guestName.trim(),
            guestEmail = guestEmail.trim(),
            guestPhone = guestPhone.trim(),
            checkInEpochDay = checkInEpochDay,
            checkOutEpochDay = checkOutEpochDay,
            syncStatus = SyncStatus.PENDING_SYNC.name,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        database.bookingDao().update(updated)

        val useFirestore = !BuildConfig.USE_KTOR_API &&
            isOnline && networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
        if (useFirestore) {
            val reference = existing.bookingReference.ifBlank { SyncRepository.formatReference(room.propertyId, bookingId) }
            val synced = updated.copy(syncStatus = SyncStatus.SYNCED.name, bookingReference = reference)
            runCatching { firestore.upsertBooking(synced) }
                .onSuccess { database.bookingDao().update(synced) }
        } else if (isOnline) {
            runCatching { syncRepository.value.syncNow() }
        } else {
            syncRepository.value.enqueueSyncWorker()
        }
        return Result.success(Unit)
    }

    suspend fun cancelBooking(bookingId: Long) {
        val session = authRepository.currentSession() ?: return
        val booking = database.bookingDao().getById(bookingId) ?: return
        if (!session.canAccessProperty(booking.propertyId)) return
        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED.name)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.updateBookingStatus(bookingId, BookingStatus.CANCELLED.name) }
        }
    }

    suspend fun checkInBooking(bookingId: Long, todayEpochDay: Long = LocalDate.now().toEpochDay()): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val booking = database.bookingDao().getById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Booking not found"))
        if (!session.canAccessProperty(booking.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (booking.status != BookingStatus.CONFIRMED.name) {
            return Result.failure(IllegalStateException("Only confirmed bookings can be checked in"))
        }
        if (booking.checkInEpochDay > todayEpochDay) {
            return Result.failure(IllegalStateException("Check-in is not available until arrival date"))
        }
        database.bookingDao().updateStatus(bookingId, BookingStatus.CHECKED_IN.name)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.updateBookingStatus(bookingId, BookingStatus.CHECKED_IN.name) }
        }
        return Result.success(Unit)
    }

    suspend fun checkOutBooking(bookingId: Long, todayEpochDay: Long = LocalDate.now().toEpochDay()): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val booking = database.bookingDao().getById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Booking not found"))
        if (!session.canAccessProperty(booking.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (booking.status != BookingStatus.CHECKED_IN.name) {
            return Result.failure(IllegalStateException("Only checked-in guests can be checked out"))
        }
        if (booking.checkOutEpochDay > todayEpochDay) {
            return Result.failure(IllegalStateException("Check-out is not available until departure date"))
        }
        database.bookingDao().updateStatus(bookingId, BookingStatus.CHECKED_OUT.name)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.updateBookingStatus(bookingId, BookingStatus.CHECKED_OUT.name) }
        }
        return Result.success(Unit)
    }
}
