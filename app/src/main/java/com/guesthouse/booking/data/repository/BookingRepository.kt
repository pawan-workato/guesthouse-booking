package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.AuditAction
import com.guesthouse.booking.data.local.entities.AuditEntityType
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingSource
import com.guesthouse.booking.data.local.entities.HousekeepingStatus
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
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
    private val syncRepository: Lazy<SyncRepository> = lazy { error("SyncRepository not initialized") },
    private val auditRepository: AuditRepository? = null
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
        isOnline: Boolean,
        source: String = BookingSource.WALK_IN.name,
        maintenanceNotes: String = ""
    ): Result<BookingCreateOutcome> {
        if (guestName.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        if (checkOutEpochDay <= checkInEpochDay) return Result.failure(IllegalArgumentException("Check-out must be after check-in"))
        val room = database.roomDao().getById(roomId) ?: return Result.failure(IllegalArgumentException("Room not found"))
        if (database.bookingDao().findOverlapping(roomId, checkInEpochDay, checkOutEpochDay).isNotEmpty()) {
            return Result.failure(IllegalStateException("Room is not available for those dates"))
        }

        val useFirestore = isOnline && networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
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
                syncStatus = syncStatus,
                source = source,
                maintenanceNotes = maintenanceNotes.trim()
            )
        )

        if (useFirestore) {
            val reference = SyncRepository.formatReference(room.propertyId, id)
            database.bookingDao().updateSync(id, SyncStatus.SYNCED.name, reference)
            database.bookingDao().getById(id)?.let { runCatching { firestore.upsertBooking(it) } }
            auditRepository?.append(AuditAction.CREATE, AuditEntityType.BOOKING, "Created booking for ${guestName.trim()} (${room.name})", room.propertyId, id)
            return Result.success(BookingCreateOutcome(id, reference, savedOffline = false))
        }

        val offlineRef = SyncRepository.formatOfflineReference(id)
        database.bookingDao().updateSync(id, SyncStatus.PENDING_SYNC.name, offlineRef)
        if (isOnline) runCatching { syncRepository.value.syncNow() }
        else syncRepository.value.enqueueSyncWorker()
        auditRepository?.append(AuditAction.CREATE, AuditEntityType.BOOKING, "Created booking for ${guestName.trim()} (${room.name})", room.propertyId, id)
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
        isOnline: Boolean,
        source: String? = null,
        maintenanceNotes: String? = null
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
            source = source ?: existing.source,
            maintenanceNotes = (maintenanceNotes ?: existing.maintenanceNotes).trim(),
            syncStatus = SyncStatus.PENDING_SYNC.name,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        database.bookingDao().update(updated)

        val useFirestore = isOnline && networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
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
        auditRepository?.append(AuditAction.UPDATE, AuditEntityType.BOOKING, "Updated booking #${bookingId} for ${guestName.trim()}", room.propertyId, bookingId)
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
        auditRepository?.append(AuditAction.CANCEL, AuditEntityType.BOOKING, "Cancelled booking #${bookingId} (${booking.guestName})", booking.propertyId, bookingId)
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
        auditRepository?.append(AuditAction.CHECK_IN, AuditEntityType.BOOKING, "Checked in booking #${bookingId} (${booking.guestName})", booking.propertyId, bookingId)
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
        database.bookingDao().updateStatus(bookingId, BookingStatus.CHECKED_OUT.name)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.updateBookingStatus(bookingId, BookingStatus.CHECKED_OUT.name) }
        }
        auditRepository?.append(AuditAction.CHECK_OUT, AuditEntityType.BOOKING, "Checked out booking #${bookingId} (${booking.guestName})", booking.propertyId, bookingId)
        return Result.success(Unit)
    }


    suspend fun extendCheckout(
        bookingId: Long,
        newCheckOutEpochDay: Long,
        isOnline: Boolean
    ): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = database.bookingDao().getById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Booking not found"))
        if (!session.canAccessProperty(existing.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (existing.status != BookingStatus.CONFIRMED.name &&
            existing.status != BookingStatus.CHECKED_IN.name
        ) {
            return Result.failure(IllegalStateException("Only active bookings can be extended"))
        }
        if (newCheckOutEpochDay <= existing.checkOutEpochDay) {
            return Result.failure(IllegalArgumentException("New check-out must be after the current check-out"))
        }
        if (newCheckOutEpochDay <= existing.checkInEpochDay) {
            return Result.failure(IllegalArgumentException("Check-out must be after check-in"))
        }
        if (database.bookingDao().findOverlapping(
                existing.roomId,
                existing.checkOutEpochDay,
                newCheckOutEpochDay,
                excludeId = bookingId
            ).isNotEmpty()
        ) {
            return Result.failure(IllegalStateException("Room is not available for the extended dates"))
        }
        if (database.blockDateDao().findOverlapping(
                existing.roomId,
                existing.checkOutEpochDay,
                newCheckOutEpochDay
            ).isNotEmpty()
        ) {
            return Result.failure(IllegalStateException("Extended dates overlap a blocked period"))
        }

        val updated = existing.copy(
            checkOutEpochDay = newCheckOutEpochDay,
            syncStatus = SyncStatus.PENDING_SYNC.name,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        database.bookingDao().update(updated)

        val useFirestore = isOnline && networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
        if (useFirestore) {
            val reference = existing.bookingReference.ifBlank {
                SyncRepository.formatReference(existing.propertyId, bookingId)
            }
            val synced = updated.copy(syncStatus = SyncStatus.SYNCED.name, bookingReference = reference)
            runCatching { firestore.upsertBooking(synced) }
                .onSuccess { database.bookingDao().update(synced) }
        } else if (isOnline) {
            runCatching { syncRepository.value.syncNow() }
        } else {
            syncRepository.value.enqueueSyncWorker()
        }
        auditRepository?.append(AuditAction.UPDATE, AuditEntityType.BOOKING, "Extended booking #${bookingId} checkout", existing.propertyId, bookingId)
        return Result.success(Unit)
    }

    suspend fun updateRoomHousekeeping(roomId: Long, status: String): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val room = database.roomDao().getById(roomId)
            ?: return Result.failure(IllegalArgumentException("Room not found"))
        if (!session.canAccessProperty(room.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        runCatching { HousekeepingStatus.valueOf(status) }.getOrElse {
            return Result.failure(IllegalArgumentException("Invalid housekeeping status"))
        }
        database.roomDao().update(room.copy(housekeepingStatus = status))
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.upsertRoom(room.copy(housekeepingStatus = status)) }
        }
        return Result.success(Unit)
    }

    suspend fun bulkCheckIn(bookingIds: List<Long>, todayEpochDay: Long): Int {
        var count = 0
        bookingIds.forEach { id ->
            checkInBooking(id, todayEpochDay).onSuccess { count++ }
        }
        return count
    }

    suspend fun bulkCheckOut(bookingIds: List<Long>, todayEpochDay: Long): Int {
        var count = 0
        bookingIds.forEach { id ->
            checkOutBooking(id, todayEpochDay).onSuccess { count++ }
        }
        return count
    }

    suspend fun createRoom(
        propertyId: Long,
        name: String,
        description: String,
        pricePerNight: Double,
        capacity: Int,
        roomType: String
    ): Result<Long> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!session.canAccessProperty(propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Room name is required"))
        if (pricePerNight < 0) return Result.failure(IllegalArgumentException("Price must be zero or greater"))
        if (capacity < 1) return Result.failure(IllegalArgumentException("Capacity must be at least 1"))
        val normalizedType = RoomType.fromStored(roomType).name

        val entity = RoomEntity(
            propertyId = propertyId,
            name = trimmedName,
            description = description.trim(),
            pricePerNight = pricePerNight,
            capacity = capacity,
            roomType = normalizedType
        )
        val id = database.roomDao().insert(entity)
        val saved = entity.copy(id = id)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.upsertRoom(saved) }
        }
        auditRepository?.append(AuditAction.CREATE, AuditEntityType.ROOM, "Created room ${trimmedName}", propertyId, id)
        return Result.success(id)
    }

    suspend fun updateRoom(room: RoomEntity): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!session.canAccessProperty(room.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (room.name.isBlank()) return Result.failure(IllegalArgumentException("Room name is required"))
        if (room.pricePerNight < 0) return Result.failure(IllegalArgumentException("Price must be zero or greater"))
        if (room.capacity < 1) return Result.failure(IllegalArgumentException("Capacity must be at least 1"))
        val updated = room.copy(
            name = room.name.trim(),
            description = room.description.trim(),
            roomType = RoomType.fromStored(room.roomType).name
        )

        database.roomDao().update(updated)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching { firestore.upsertRoom(updated) }
        }
        auditRepository?.append(AuditAction.UPDATE, AuditEntityType.ROOM, "Updated room ${updated.name}", updated.propertyId, updated.id)
        return Result.success(Unit)
    }
}
