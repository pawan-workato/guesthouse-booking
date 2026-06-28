package com.guesthouse.booking.data.repository

import com.guesthouse.booking.BuildConfig
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow

class GuestRepository(
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    private val firestore: FirestoreDataSource = FirestoreDataSource(),
    private val syncRepository: Lazy<SyncRepository> = lazy { error("SyncRepository not initialized") }
) {
    fun observeActiveGuests(): Flow<List<GuestEntity>> = database.guestDao().observeActive()
    fun observeAllGuests(): Flow<List<GuestEntity>> = database.guestDao().observeAllIncludingInactive()
    fun observeGuest(guestId: Long): Flow<GuestEntity?> = database.guestDao().observeById(guestId)
    suspend fun getGuest(guestId: Long): GuestEntity? = database.guestDao().getById(guestId)

    private suspend fun enqueueKtorSyncIfNeeded() {
        if (!BuildConfig.USE_KTOR_API) return
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { syncRepository.value.syncNow() }
        } else {
            syncRepository.value.enqueueSyncWorker()
        }
    }


    suspend fun createGuest(name: String, email: String, phone: String, notes: String): Result<Long> {
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
        if (BuildConfig.USE_KTOR_API) {
            enqueueKtorSyncIfNeeded()
        } else if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching {
                firestore.upsertGuest(saved.copy(syncStatus = SyncStatus.SYNCED.name))
                database.guestDao().update(saved.copy(syncStatus = SyncStatus.SYNCED.name))
            }
        }
        return Result.success(id)
    }

    suspend fun updateGuest(guest: GuestEntity): Result<Unit> {
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
        if (BuildConfig.USE_KTOR_API) {
            enqueueKtorSyncIfNeeded()
        } else if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching {
                firestore.upsertGuest(updated.copy(syncStatus = SyncStatus.SYNCED.name))
                database.guestDao().update(updated.copy(syncStatus = SyncStatus.SYNCED.name))
            }
        }
        return Result.success(Unit)
    }

    suspend fun setGuestActive(guestId: Long, active: Boolean) {
        val guest = database.guestDao().getById(guestId) ?: return
        val updated = guest.copy(
            isActive = active,
            syncStatus = SyncStatus.PENDING_SYNC.name,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        database.guestDao().update(updated)
        if (BuildConfig.USE_KTOR_API) {
            enqueueKtorSyncIfNeeded()
        } else if (networkMonitor.isCurrentlyOnline() && firestore.isSignedIn) {
            runCatching {
                firestore.upsertGuest(updated.copy(syncStatus = SyncStatus.SYNCED.name))
                database.guestDao().update(updated.copy(syncStatus = SyncStatus.SYNCED.name))
            }
        }
    }
}
