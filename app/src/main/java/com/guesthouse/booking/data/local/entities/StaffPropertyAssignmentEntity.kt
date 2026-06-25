package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "staff_property_assignments",
    primaryKeys = ["staffId", "propertyId"],
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("staffId"), Index("propertyId")]
)
data class StaffPropertyAssignmentEntity(
    val staffId: Long,
    val propertyId: Long
)
