package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.guest.GuestMatching
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class GuestStayBooking(
    val booking: BookingEntity,
    val propertyName: String,
    val roomName: String
)

class GuestRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val firestore: FirestoreDataSource = FirestoreDataSource()
) {
    fun observeActiveGuests(): Flow<List<GuestEntity>> = observeScopedActiveGuests()

    fun observeAllGuests(): Flow<List<GuestEntity>> = observeScopedAllGuests()

    fun observeScopedActiveGuests(): Flow<List<GuestEntity>> = scopedGuestFlow { database.guestDao().observeActive() }

    fun observeScopedAllGuests(): Flow<List<GuestEntity>> =
        scopedGuestFlow { database.guestDao().observeAllIncludingInactive() }

    fun observeGuest(guestId: Long): Flow<GuestEntity?> =
        authRepository.session.flatMapLatest { session ->
            if (session == null) flowOf(null)
            else database.guestDao().observeById(guestId)
        }

    suspend fun getGuest(guestId: Long): GuestEntity? {
        if (!canViewGuest(guestId)) return null
        return database.guestDao().getById(guestId)
    }

    suspend fun canViewGuest(guestId: Long): Boolean {
        if (authRepository.currentSession() == null) return false
        return database.guestDao().getById(guestId) != null
    }

    suspend fun canEditGuest(guestId: Long): Boolean = canViewGuest(guestId)

    suspend fun canDeleteGuest(guestId: Long): Boolean {
        val session = authRepository.currentSession() ?: return false
        return session.isChainAdmin && canViewGuest(guestId)
    }

    suspend fun canAccessGuest(guestId: Long): Boolean = canViewGuest(guestId)

    /** Stay history scoped by role: chain admin sees all properties; managers see assigned properties only. */
    fun observeGuestStayHistory(guestId: Long): Flow<List<GuestStayBooking>> =
        authRepository.session.flatMapLatest { session ->
            if (session == null) {
                return@flatMapLatest flowOf(emptyList())
            }
            val bookingsFlow = when {
                session.isChainAdmin -> database.bookingDao().observeForGuest(guestId)
                session.assignedPropertyIds.isEmpty() -> flowOf(emptyList())
                else -> database.bookingDao().observeForGuestAtProperties(
                    guestId,
                    session.assignedPropertyIds.toList()
                )
            }
            combine(
                bookingsFlow,
                database.propertyDao().observeAll(),
                database.roomDao().observeAll()
            ) { bookings, properties, rooms ->
                val propertyMap = properties.associateBy { it.id }
                val roomMap = rooms.associateBy { it.id }
                bookings.map { booking ->
                    GuestStayBooking(
                        booking = booking,
                        propertyName = propertyMap[booking.propertyId]?.name ?: "Unknown property",
                        roomName = roomMap[booking.roomId]?.name ?: "Unknown room"
                    )
                }
            }
        }

    private fun scopedGuestFlow(allGuests: () -> Flow<List<GuestEntity>>): Flow<List<GuestEntity>> =
        authRepository.session.flatMapLatest { session ->
            if (session == null) flowOf(emptyList()) else allGuests()
        }


    suspend fun findSimilarGuests(
        name: String,
        email: String,
        phone: String,
        excludeGuestId: Long? = null
    ): List<GuestEntity> {
        if (authRepository.currentSession() == null) return emptyList()
        val trimmedName = name.trim()
        val normalizedEmail = GuestMatching.normalizeEmail(email)
        val normalizedPhone = GuestMatching.normalizePhone(phone)
        if (trimmedName.length < 2 && normalizedEmail.isBlank() && normalizedPhone.length < 7) {
            return emptyList()
        }
        return database.guestDao().getAllActive()
            .filter { guest ->
                guest.id != excludeGuestId &&
                    GuestMatching.matches(guest, name, email, phone)
            }
            .take(5)
    }

    suspend fun createGuest(name: String, email: String, phone: String, notes: String): Result<Long> {
        if (authRepository.currentSession() == null) {
            return Result.failure(IllegalStateException("Not signed in"))
        }
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        val entity = GuestEntity(
            name = trimmedName,
            email = email.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            isActive = true,
            syncStatus = SyncStatus.PENDING_SYNC.name
        )
        val id = database.guestDao().insert(entity)
        val saved = entity.copy(id = id)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching {
                firestore.upsertGuest(saved.copy(syncStatus = SyncStatus.SYNCED.name))
                database.guestDao().update(saved.copy(syncStatus = SyncStatus.SYNCED.name))
            }
        }
        return Result.success(id)
    }

    suspend fun updateGuest(guest: GuestEntity): Result<Unit> {
        if (!canEditGuest(guest.id)) {
            return Result.failure(IllegalStateException("You don't have access to this guest"))
        }
        if (guest.name.isBlank()) return Result.failure(IllegalArgumentException("Guest name is required"))
        val updated = guest.copy(
            name = guest.name.trim(),
            email = guest.email.trim(),
            phone = guest.phone.trim(),
            notes = guest.notes.trim(),
            syncStatus = SyncStatus.PENDING_SYNC.name,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        database.guestDao().update(updated)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching {
                firestore.upsertGuest(updated.copy(syncStatus = SyncStatus.SYNCED.name))
                database.guestDao().update(updated.copy(syncStatus = SyncStatus.SYNCED.name))
            }
        }
        return Result.success(Unit)
    }

    suspend fun setGuestActive(guestId: Long, active: Boolean) {
        if (!canDeleteGuest(guestId)) return
        val guest = database.guestDao().getById(guestId) ?: return
        val updated = guest.copy(
            isActive = active,
            syncStatus = SyncStatus.PENDING_SYNC.name,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        database.guestDao().update(updated)
        if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching {
                firestore.upsertGuest(updated.copy(syncStatus = SyncStatus.SYNCED.name))
                database.guestDao().update(updated.copy(syncStatus = SyncStatus.SYNCED.name))
            }
        }
    }
}
