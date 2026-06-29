package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.HandoverNoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HandoverNoteRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) {
    fun observeNotes(propertyId: Long?): Flow<List<HandoverNoteEntity>> =
        authRepository.session.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(emptyList())
            when {
                propertyId != null && session.canAccessProperty(propertyId) ->
                    database.handoverNoteDao().observeForProperty(propertyId)
                session.isChainAdmin && propertyId == null ->
                    database.handoverNoteDao().observeAll()
                propertyId == null && session.assignedPropertyIds.isNotEmpty() ->
                    database.handoverNoteDao().observeAll()
                else -> flowOf(emptyList())
            }
        }

    suspend fun addNote(propertyId: Long, note: String): Result<Unit> {
        val session = authRepository.currentSession()
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!session.canAccessProperty(propertyId)) {
            return Result.failure(IllegalStateException("You don't have access to this property"))
        }
        val trimmed = note.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Note is required"))
        database.handoverNoteDao().insert(
            HandoverNoteEntity(
                propertyId = propertyId,
                staffId = session.staffId,
                staffName = session.displayName,
                note = trimmed
            )
        )
        return Result.success(Unit)
    }
}
