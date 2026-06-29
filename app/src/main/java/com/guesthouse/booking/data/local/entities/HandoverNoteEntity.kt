package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "handover_notes",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("propertyId"), Index("createdAtEpochMs")]
)
data class HandoverNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val staffId: Long,
    val staffName: String,
    val note: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
