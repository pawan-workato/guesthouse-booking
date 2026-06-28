package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val role: String,
    val firebaseUid: String = "",
    val isActive: Boolean = true
)
