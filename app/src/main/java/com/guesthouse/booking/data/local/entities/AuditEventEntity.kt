package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_events",
    indices = [Index("propertyId"), Index("createdAtEpochMs")]
)
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val staffName: String,
    val propertyId: Long? = null,
    val action: String,
    val entityType: String,
    val entityId: Long? = null,
    val summary: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

object AuditAction {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
    const val CANCEL = "CANCEL"
    const val CHECK_IN = "CHECK_IN"
    const val CHECK_OUT = "CHECK_OUT"
    const val ACTIVATE = "ACTIVATE"
    const val DEACTIVATE = "DEACTIVATE"
}

object AuditEntityType {
    const val BOOKING = "BOOKING"
    const val GUEST = "GUEST"
    const val PROPERTY = "PROPERTY"
    const val ROOM = "ROOM"
    const val BLOCK_DATE = "BLOCK_DATE"
}
