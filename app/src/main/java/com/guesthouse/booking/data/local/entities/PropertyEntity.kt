package com.guesthouse.booking.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val region: String,
    val checkInTime: String = "15:00",
    val checkOutTime: String = "11:00"
)
