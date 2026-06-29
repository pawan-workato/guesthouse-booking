package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import kotlinx.coroutines.flow.Flow

class BlockDateRepository(private val database: AppDatabase, private val authRepository: AuthRepository) {
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
        val id = database.blockDateDao().insert(
            BlockDateEntity(
                propertyId = room.propertyId,
                roomId = roomId,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
                reason = reason.trim(),
                createdByStaffId = session.staffId
            )
        )
        return Result.success(id)
    }

    suspend fun removeBlock(blockId: Long): Result<Unit> {
        val session = authRepository.currentSession() ?: return Result.failure(IllegalStateException("Not signed in"))
        val block = database.blockDateDao().getById(blockId) ?: return Result.failure(IllegalArgumentException("Block not found"))
        if (!session.canAccessProperty(block.propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        database.blockDateDao().deleteById(blockId)
        return Result.success(Unit)
    }
}
