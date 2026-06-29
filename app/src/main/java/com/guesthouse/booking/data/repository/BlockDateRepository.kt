package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.firebase.FirestoreDataSource
import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.AuditAction
import com.guesthouse.booking.data.local.entities.AuditEntityType
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow

class BlockDateRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val firestore: FirestoreDataSource = FirestoreDataSource(),
    private val syncRepository: Lazy<SyncRepository> = lazy { error("SyncRepository not initialized") },
    private val auditRepository: AuditRepository? = null
) {
    fun observeForRoom(roomId: Long): Flow<List<BlockDateEntity>> = database.blockDateDao().observeForRoom(roomId)

    suspend fun createBlock(roomId: Long, startEpochDay: Long, endEpochDay: Long, reason: String): Result<Long> {
        val session = authRepository.currentSession() ?: return Result.failure(IllegalStateException("Not signed in"))
        val room = database.roomDao().getById(roomId) ?: return Result.failure(IllegalArgumentException("Room not found"))
        if (!session.canAccessProperty(room.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        if (endEpochDay <= startEpochDay) return Result.failure(IllegalArgumentException("End date must be after start date"))
        if (database.blockDateDao().findOverlapping(roomId, startEpochDay, endEpochDay).isNotEmpty()) {
            return Result.failure(IllegalStateException("These dates overlap an existing block"))
        }
        if (database.bookingDao().findOverlapping(roomId, startEpochDay, endEpochDay).isNotEmpty()) {
            return Result.failure(IllegalStateException("These dates overlap an existing booking"))
        }

        val useFirestore = networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
        val syncStatus = if (useFirestore) SyncStatus.SYNCED.name else SyncStatus.PENDING_SYNC.name
        val id = database.blockDateDao().insert(
            BlockDateEntity(
                propertyId = room.propertyId,
                roomId = roomId,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
                reason = reason.trim(),
                createdByStaffId = session.staffId,
                syncStatus = syncStatus
            )
        )

        if (useFirestore) {
            database.blockDateDao().getById(id)?.let { runCatching { firestore.upsertBlockDate(it) } }
        } else {
            syncRepository.value.enqueueSyncWorker()
        }
        auditRepository?.append(AuditAction.CREATE, AuditEntityType.BLOCK_DATE, "Blocked room dates: ${reason.trim()}", room.propertyId, id)
        return Result.success(id)
    }

    suspend fun removeBlock(blockId: Long): Result<Unit> {
        val session = authRepository.currentSession() ?: return Result.failure(IllegalStateException("Not signed in"))
        val block = database.blockDateDao().getById(blockId) ?: return Result.failure(IllegalArgumentException("Block not found"))
        if (!session.canAccessProperty(block.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }

        val useFirestore = networkMonitor.isCurrentlyOnline() && firestore.isSignedIn
        if (block.syncStatus == SyncStatus.PENDING_SYNC.name && !block.markedForDeletion) {
            database.blockDateDao().deleteById(blockId)
            return Result.success(Unit)
        }
        if (useFirestore) {
            runCatching { firestore.deleteBlockDate(blockId) }
            database.blockDateDao().deleteById(blockId)
        } else {
            database.blockDateDao().markForDeletion(blockId, SyncStatus.PENDING_SYNC.name)
            syncRepository.value.enqueueSyncWorker()
        }
        auditRepository?.append(AuditAction.DELETE, AuditEntityType.BLOCK_DATE, "Removed block date #${blockId}", block.propertyId, blockId)
        return Result.success(Unit)
    }
}
