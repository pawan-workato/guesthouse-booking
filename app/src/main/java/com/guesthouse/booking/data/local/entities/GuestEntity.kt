package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guests")
data class GuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
