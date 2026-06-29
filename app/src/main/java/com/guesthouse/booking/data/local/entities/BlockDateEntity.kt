package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_dates",
    foreignKeys = [
        ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RoomEntity::class, parentColumns = ["id"], childColumns = ["roomId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("propertyId"), Index("roomId"), Index("syncStatus")]
)
data class BlockDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val roomId: Long,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val reason: String = "",
    val createdByStaffId: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED.name,
    val markedForDeletion: Boolean = false
)
