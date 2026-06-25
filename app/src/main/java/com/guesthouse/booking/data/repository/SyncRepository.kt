package com.guesthouse.booking.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
    val noNetwork: Boolean = false
)

class SyncRepository(
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    context: Context
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _lastSyncEpochMs = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))
    val lastSyncEpochMs: StateFlow<Long> = _lastSyncEpochMs.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val pendingCount: Flow<Int> =
        database.bookingDao().observeCountBySyncStatus(SyncStatus.PENDING_SYNC.name)

    val conflictCount: Flow<Int> =
        database.bookingDao().observeCountBySyncStatus(SyncStatus.CONFLICT.name)

    val issueCount: Flow<Int> = combine(pendingCount, conflictCount) { pending, conflicts ->
        pending + conflicts
    }

    fun observeConflicts() =
        database.bookingDao().observeBySyncStatus(SyncStatus.CONFLICT.name)

    fun observePending() =
        database.bookingDao().observeBySyncStatus(SyncStatus.PENDING_SYNC.name)

    suspend fun syncNow(): SyncResult {
        if (!networkMonitor.isCurrentlyOnline()) {
            return SyncResult(noNetwork = true)
        }
        val pending = database.bookingDao().getBySyncStatus(SyncStatus.PENDING_SYNC.name)
        var synced = 0
        var conflicts = 0
        for (booking in pending) {
            val overlaps = database.bookingDao().findOverlapping(
                roomId = booking.roomId,
                checkIn = booking.checkInEpochDay,
                checkOut = booking.checkOutEpochDay,
                excludeId = booking.id
            ).filter { it.syncStatus == SyncStatus.SYNCED.name }
            if (overlaps.isNotEmpty()) {
                database.bookingDao().updateSyncStatus(booking.id, SyncStatus.CONFLICT.name)
                conflicts++
            } else {
                val reference = formatReference(booking.propertyId, booking.id)
                database.bookingDao().updateSync(booking.id, SyncStatus.SYNCED.name, reference)
                synced++
            }
        }
        if (pending.isNotEmpty()) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
            _lastSyncEpochMs.value = now
        }
        return SyncResult(syncedCount = synced, conflictCount = conflicts)
    }

    suspend fun dismissConflict(bookingId: Long) {
        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED.name)
        database.bookingDao().updateSyncStatus(bookingId, SyncStatus.SYNCED.name)
    }

    fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
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
