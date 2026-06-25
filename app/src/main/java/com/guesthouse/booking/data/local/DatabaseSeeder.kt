package com.guesthouse.booking.data.local

import com.guesthouse.booking.data.local.entities.RoomEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseSeeder {
    fun seedIfEmpty(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            if (database.roomDao().count() > 0) return@launch
            database.roomDao().insertAll(
                listOf(
                    RoomEntity(
                        name = "Garden Suite",
                        description = "Ground-floor room with private patio and garden views.",
                        pricePerNight = 89.0,
                        capacity = 2
                    ),
                    RoomEntity(
                        name = "Loft Room",
                        description = "Bright upper-floor room with skylight and workspace.",
                        pricePerNight = 75.0,
                        capacity = 2
                    ),
                    RoomEntity(
                        name = "Family Room",
                        description = "Spacious room with two queen beds, ideal for families.",
                        pricePerNight = 120.0,
                        capacity = 4
                    ),
                    RoomEntity(
                        name = "Cozy Single",
                        description = "Compact single room perfect for solo travelers.",
                        pricePerNight = 55.0,
                        capacity = 1
                    )
                )
            )
        }
    }
}
