package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.guesthouse.booking.data.local.entities.SyncStatus

@Entity(tableName = "guests")
data class GuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val serverId: Long? = null,
    val syncStatus: String = SyncStatus.SYNCED.name,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
