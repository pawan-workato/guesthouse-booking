package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val pricePerNight: Double,
    val capacity: Int
)
