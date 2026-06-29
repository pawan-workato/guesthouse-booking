package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.AuditEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuditRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) {
    fun observeEvents(): Flow<List<AuditEventEntity>> =
        authRepository.session.flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else if (session.isChainAdmin) {
                database.auditEventDao().observeAll()
            } else if (session.assignedPropertyIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                database.auditEventDao().observeForProperties(session.assignedPropertyIds.toList())
            }
        }

    suspend fun append(
        action: String,
        entityType: String,
        summary: String,
        propertyId: Long? = null,
        entityId: Long? = null
    ) {
        val session = authRepository.currentSession() ?: return
        database.auditEventDao().insert(
            AuditEventEntity(
                staffId = session.staffId,
                staffName = session.displayName,
                propertyId = propertyId,
                action = action,
                entityType = entityType,
                entityId = entityId,
                summary = summary
            )
        )
    }
}
