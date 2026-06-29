package com.guesthouse.booking.data.repository

import android.content.Context
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.guesthouse.booking.data.firebase.FirebaseInitializer
import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.firebase.FirestoreSyncService
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.sync.NetworkMonitor
import com.guesthouse.booking.worker.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit

data class SyncResult(
    val syncedCount: Int = 0,
    val conflictCount: Int = 0,
    val propertiesPulled: Int = 0,
    val guestsPulled: Int = 0,
    val pullErrors: List<String> = emptyList(),
    val noNetwork: Boolean = false,
    val notAuthenticated: Boolean = false
)

class SyncRepository(
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    context: Context,
    private val authRepository: AuthRepository,
    private val firestore: FirestoreDataSource = FirestoreDataSource(),
    private val syncService: FirestoreSyncService = FirestoreSyncService(database, firestore)
) {
    private val appContext = context.applicationContext
    private val firebaseEnabled = FirebaseInitializer.isConfigured(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _lastSyncEpochMs = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))
    val lastSyncEpochMs: StateFlow<Long> = _lastSyncEpochMs.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val pendingCount: Flow<Int> = combine(
        database.bookingDao().observeCountBySyncStatus(SyncStatus.PENDING_SYNC.name),
        database.blockDateDao().observeCountBySyncStatus(SyncStatus.PENDING_SYNC.name)
    ) { bookingPending, blockPending -> bookingPending + blockPending }

    val conflictCount: Flow<Int> = combine(
        database.bookingDao().observeCountBySyncStatus(SyncStatus.CONFLICT.name),
        database.blockDateDao().observeCountBySyncStatus(SyncStatus.CONFLICT.name)
    ) { bookingConflicts, blockConflicts -> bookingConflicts + blockConflicts }

    val issueCount: Flow<Int> = combine(pendingCount, conflictCount) { pending, conflicts ->
        pending + conflicts
    }

    fun observeConflicts() = database.bookingDao().observeBySyncStatus(SyncStatus.CONFLICT.name)
    fun observePending() = database.bookingDao().observeBySyncStatus(SyncStatus.PENDING_SYNC.name)

    suspend fun syncNow(): SyncResult {
        if (!networkMonitor.isCurrentlyOnline()) return SyncResult(noNetwork = true)
        val session = authRepository.currentSession()
        if (firebaseEnabled) {
            if (!firestore.isSignedIn || session == null) {
                return SyncResult(notAuthenticated = true)
            }
            return syncWithFirestore(session)
        }
        return syncLocalOnly()
    }

    private suspend fun syncWithFirestore(session: com.guesthouse.booking.data.auth.StaffSession): SyncResult {
        var syncedCount = 0
        var conflictCount = 0
        var hadActivity = false
        val pending = database.bookingDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
        for (booking in pending) {
            hadActivity = true
            val localOverlaps = database.bookingDao().findOverlapping(
                roomId = booking.roomId,
                checkIn = booking.checkInEpochDay,
                checkOut = booking.checkOutEpochDay,
                excludeId = booking.id
            ).filter { it.syncStatus == SyncStatus.SYNCED.name }
            val remoteOverlaps = runCatching {
                firestore.findOverlappingRemoteBookings(
                    roomId = booking.roomId,
                    checkIn = booking.checkInEpochDay,
                    checkOut = booking.checkOutEpochDay,
                    excludeId = booking.id
                )
            }.getOrDefault(emptyList())
            if (localOverlaps.isNotEmpty() || remoteOverlaps.isNotEmpty()) {
                database.bookingDao().updateSyncStatus(booking.id, SyncStatus.CONFLICT.name)
                conflictCount++
            } else {
                val reference = formatReference(booking.propertyId, booking.id)
                val synced = booking.copy(syncStatus = SyncStatus.SYNCED.name, bookingReference = reference)
                runCatching { firestore.upsertBooking(synced) }
                    .onSuccess {
                        database.bookingDao().updateSync(synced.id, SyncStatus.SYNCED.name, reference)
                        syncedCount++
                    }
                    .onFailure {
                        database.bookingDao().updateSyncStatus(booking.id, SyncStatus.PENDING_SYNC.name)
                    }
            }
        }
        val pendingBlocks = database.blockDateDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
        for (block in pendingBlocks) {
            hadActivity = true
            if (block.markedForDeletion) {
                if (block.syncStatus == SyncStatus.PENDING_SYNC.name) {
                    runCatching { firestore.deleteBlockDate(block.id) }
                        .onSuccess { database.blockDateDao().deleteById(block.id) }
                }
                continue
            }
            val localOverlaps = database.blockDateDao().findOverlapping(
                roomId = block.roomId,
                startEpochDay = block.startEpochDay,
                endEpochDay = block.endEpochDay,
                excludeId = block.id
            ).filter { it.syncStatus == SyncStatus.SYNCED.name }
            val remoteOverlaps = runCatching {
                firestore.findOverlappingRemoteBlockDates(
                    roomId = block.roomId,
                    startEpochDay = block.startEpochDay,
                    endEpochDay = block.endEpochDay,
                    excludeId = block.id
                )
            }.getOrDefault(emptyList())
            if (localOverlaps.isNotEmpty() || remoteOverlaps.isNotEmpty()) {
                database.blockDateDao().updateSyncStatus(block.id, SyncStatus.CONFLICT.name)
                conflictCount++
            } else {
                val synced = block.copy(syncStatus = SyncStatus.SYNCED.name)
                runCatching { firestore.upsertBlockDate(synced) }
                    .onSuccess {
                        database.blockDateDao().updateSyncStatus(synced.id, SyncStatus.SYNCED.name)
                        syncedCount++
                    }
                    .onFailure {
                        database.blockDateDao().updateSyncStatus(block.id, SyncStatus.PENDING_SYNC.name)
                    }
            }
        }
        val pullResult = syncService.pullRemoteData(session)
        if (hadActivity || syncedCount > 0 || pullResult.hasData) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
            _lastSyncEpochMs.value = now
        }
        return SyncResult(
            syncedCount = syncedCount,
            conflictCount = conflictCount,
            propertiesPulled = pullResult.propertiesCount,
            guestsPulled = pullResult.guestsCount,
            pullErrors = pullResult.errors
        )
    }

    private suspend fun syncLocalOnly(): SyncResult {
        val (synced, conflicts, hadPending) = database.withTransaction {
            val pending = database.bookingDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
            var syncedCount = 0
            var conflictCount = 0
            for (booking in pending) {
                val overlaps = database.bookingDao().findOverlapping(
                    roomId = booking.roomId,
                    checkIn = booking.checkInEpochDay,
                    checkOut = booking.checkOutEpochDay,
                    excludeId = booking.id
                ).filter { it.syncStatus == SyncStatus.SYNCED.name }
                if (overlaps.isNotEmpty()) {
                    database.bookingDao().updateSyncStatus(booking.id, SyncStatus.CONFLICT.name)
                    conflictCount++
                } else {
                    val reference = formatReference(booking.propertyId, booking.id)
                    database.bookingDao().updateSync(booking.id, SyncStatus.SYNCED.name, reference)
                    syncedCount++
                }
            }
            val pendingBlocks = database.blockDateDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
            for (block in pendingBlocks) {
                if (block.markedForDeletion) {
                    database.blockDateDao().deleteById(block.id)
                    syncedCount++
                    continue
                }
                val overlaps = database.blockDateDao().findOverlapping(
                    roomId = block.roomId,
                    startEpochDay = block.startEpochDay,
                    endEpochDay = block.endEpochDay,
                    excludeId = block.id
                ).filter { it.syncStatus == SyncStatus.SYNCED.name }
                if (overlaps.isNotEmpty()) {
                    database.blockDateDao().updateSyncStatus(block.id, SyncStatus.CONFLICT.name)
                    conflictCount++
                } else {
                    database.blockDateDao().updateSyncStatus(block.id, SyncStatus.SYNCED.name)
                    syncedCount++
                }
            }
            Triple(syncedCount, conflictCount, pending.isNotEmpty() || pendingBlocks.isNotEmpty())
        }
        if (hadPending) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
            _lastSyncEpochMs.value = now
        }
        return SyncResult(syncedCount = synced, conflictCount = conflicts)
    }

    suspend fun dismissConflict(bookingId: Long) {
        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED.name)
        database.bookingDao().updateSyncStatus(bookingId, SyncStatus.SYNCED.name)
        if (firebaseEnabled && firestore.isSignedIn) {
            runCatching { firestore.updateBookingStatus(bookingId, BookingStatus.CANCELLED.name) }
        }
    }

    fun enqueueSyncWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
        )
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        )
    }

    companion object {
        private const val PREFS_NAME = "guesthouse_sync"
        private const val KEY_LAST_SYNC = "last_sync_epoch_ms"
        const val SYNC_WORK_NAME = "guesthouse_sync_once"
        const val PERIODIC_SYNC_NAME = "guesthouse_sync_periodic"

        fun formatReference(propertyId: Long, bookingId: Long): String =
            "GH-$propertyId-${bookingId.toString().padStart(4, '0')}"

        fun formatOfflineReference(bookingId: Long): String =
            "TMP-${bookingId.toString().padStart(4, '0')}"
    }
}
