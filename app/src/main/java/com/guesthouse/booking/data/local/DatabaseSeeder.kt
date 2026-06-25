package com.guesthouse.booking.data.local

import com.guesthouse.booking.data.auth.PasswordHasher
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.StaffEntity
import com.guesthouse.booking.data.local.entities.StaffPropertyAssignmentEntity
import com.guesthouse.booking.data.local.entities.StaffRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseSeeder {
    fun seedIfEmpty(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            if (database.propertyDao().count() > 0) return@launch

            database.propertyDao().insertAll(
                listOf(
                    PropertyEntity(1, "Hill View Guesthouse", "142 Ridge Rd, Aspen, CO", "Mountain West"),
                    PropertyEntity(2, "Riverside Lodge", "88 River Walk, Portland, OR", "Pacific NW"),
                    PropertyEntity(3, "Cedar Inn", "19 Cedar Ln, Bozeman, MT", "Mountain West"),
                    PropertyEntity(4, "Harbor House", "5 Pier St, Monterey, CA", "Coastal"),
                    PropertyEntity(5, "Maple Retreat", "301 Maple Ave, Burlington, VT", "Northeast"),
                    PropertyEntity(6, "Sunstone Villa", "44 Desert Dr, Sedona, AZ", "Southwest"),
                    PropertyEntity(7, "Pinecrest Lodge", "77 Pine Rd, Jackson, WY", "Mountain West"),
                    PropertyEntity(8, "Lakeside Haven", "12 Lakeview Dr, Traverse City, MI", "Midwest"),
                    PropertyEntity(9, "Desert Bloom Inn", "210 Cactus Way, Santa Fe, NM", "Southwest"),
                    PropertyEntity(10, "Oak & Ivy Guesthouse", "56 Oak St, Asheville, NC", "Southeast"),
                    PropertyEntity(11, "Summit Stay", "901 Summit Blvd, Denver, CO", "Mountain West"),
                    PropertyEntity(12, "Meadowbrook Cottage", "3 Meadow Ln, Madison, WI", "Midwest")
                )
            )

            database.roomDao().insertAll(
                listOf(
                    RoomEntity(propertyId = 1, name = "Garden Suite", description = "Ground-floor patio and garden views.", pricePerNight = 89.0, capacity = 2),
                    RoomEntity(propertyId = 1, name = "Loft Room", description = "Upper-floor skylight and workspace.", pricePerNight = 75.0, capacity = 2),
                    RoomEntity(propertyId = 1, name = "Family Room", description = "Two queen beds for families.", pricePerNight = 120.0, capacity = 4),
                    RoomEntity(propertyId = 2, name = "River View", description = "Balcony overlooking the river.", pricePerNight = 95.0, capacity = 2),
                    RoomEntity(propertyId = 2, name = "Studio", description = "Compact studio with kitchenette.", pricePerNight = 70.0, capacity = 2),
                    RoomEntity(propertyId = 2, name = "Suite", description = "Separate living area and bedroom.", pricePerNight = 130.0, capacity = 3),
                    RoomEntity(propertyId = 3, name = "Cedar Double", description = "Warm wood finishes, mountain view.", pricePerNight = 80.0, capacity = 2),
                    RoomEntity(propertyId = 3, name = "Cozy Single", description = "Ideal for solo travelers.", pricePerNight = 55.0, capacity = 1),
                    RoomEntity(propertyId = 4, name = "Harbor King", description = "King bed with ocean glimpse.", pricePerNight = 110.0, capacity = 2),
                    RoomEntity(propertyId = 4, name = "Anchor Room", description = "Nautical theme, queen bed.", pricePerNight = 85.0, capacity = 2),
                    RoomEntity(propertyId = 4, name = "Captain's Suite", description = "Corner suite with bay windows.", pricePerNight = 145.0, capacity = 4),
                    RoomEntity(propertyId = 5, name = "Maple Standard", description = "Classic room with maple grove view.", pricePerNight = 78.0, capacity = 2),
                    RoomEntity(propertyId = 5, name = "Autumn Suite", description = "Spacious suite, fireplace.", pricePerNight = 115.0, capacity = 3),
                    RoomEntity(propertyId = 6, name = "Adobe Room", description = "Southwestern adobe styling.", pricePerNight = 92.0, capacity = 2),
                    RoomEntity(propertyId = 6, name = "Terrace Double", description = "Private terrace, red rock views.", pricePerNight = 105.0, capacity = 2),
                    RoomEntity(propertyId = 6, name = "Poolside", description = "Steps from the courtyard pool.", pricePerNight = 98.0, capacity = 2),
                    RoomEntity(propertyId = 7, name = "Pine Standard", description = "Forest-facing double room.", pricePerNight = 88.0, capacity = 2),
                    RoomEntity(propertyId = 7, name = "Bear Den", description = "Rustic lodge feel, two beds.", pricePerNight = 100.0, capacity = 4),
                    RoomEntity(propertyId = 8, name = "Lakeview Double", description = "Direct lake views.", pricePerNight = 90.0, capacity = 2),
                    RoomEntity(propertyId = 8, name = "Dock Room", description = "Near the private dock.", pricePerNight = 82.0, capacity = 2),
                    RoomEntity(propertyId = 8, name = "Family Cottage", description = "Two-bedroom cottage unit.", pricePerNight = 155.0, capacity = 6),
                    RoomEntity(propertyId = 9, name = "Bloom Single", description = "Courtyard garden access.", pricePerNight = 65.0, capacity = 1),
                    RoomEntity(propertyId = 9, name = "Adobe Double", description = "Traditional pueblo design.", pricePerNight = 85.0, capacity = 2),
                    RoomEntity(propertyId = 10, name = "Ivy Room", description = "Garden-level, wheelchair accessible.", pricePerNight = 79.0, capacity = 2),
                    RoomEntity(propertyId = 10, name = "Oak Suite", description = "Top floor with mountain views.", pricePerNight = 112.0, capacity = 3),
                    RoomEntity(propertyId = 10, name = "Carriage House", description = "Detached unit with kitchen.", pricePerNight = 135.0, capacity = 4),
                    RoomEntity(propertyId = 11, name = "Summit Double", description = "City and mountain skyline.", pricePerNight = 86.0, capacity = 2),
                    RoomEntity(propertyId = 11, name = "Alpine Room", description = "Quiet rear-facing room.", pricePerNight = 72.0, capacity = 2),
                    RoomEntity(propertyId = 12, name = "Meadow Double", description = "Pasture views, ground floor.", pricePerNight = 74.0, capacity = 2),
                    RoomEntity(propertyId = 12, name = "Brook Suite", description = "Stream-side suite with sitting area.", pricePerNight = 99.0, capacity = 3)
                )
            )

            seedStaff(database)
        }
    }

    private suspend fun seedStaff(database: AppDatabase) {
        if (database.staffDao().count() > 0) return

        val adminHash = PasswordHasher.hash("admin123")
        val managerHash = PasswordHasher.hash("manager123")

        database.staffDao().insertAll(
            listOf(
                StaffEntity(1, "admin@chain.com", adminHash, "Chain Admin", StaffRole.CHAIN_ADMIN.name),
                StaffEntity(2, "manager.mountain@chain.com", managerHash, "Alex Mountain", StaffRole.PROPERTY_MANAGER.name),
                StaffEntity(3, "manager.coastal@chain.com", managerHash, "Sam Coastal", StaffRole.PROPERTY_MANAGER.name),
                StaffEntity(4, "manager.southwest@chain.com", managerHash, "Jordan Southwest", StaffRole.PROPERTY_MANAGER.name),
                StaffEntity(5, "manager.east@chain.com", managerHash, "Taylor East", StaffRole.PROPERTY_MANAGER.name)
            )
        )

        database.staffDao().insertAssignments(
            listOf(
                StaffPropertyAssignmentEntity(2, 1),
                StaffPropertyAssignmentEntity(2, 3),
                StaffPropertyAssignmentEntity(2, 7),
                StaffPropertyAssignmentEntity(2, 11),
                StaffPropertyAssignmentEntity(3, 2),
                StaffPropertyAssignmentEntity(3, 4),
                StaffPropertyAssignmentEntity(3, 8),
                StaffPropertyAssignmentEntity(4, 6),
                StaffPropertyAssignmentEntity(4, 9),
                StaffPropertyAssignmentEntity(5, 5),
                StaffPropertyAssignmentEntity(5, 10),
                StaffPropertyAssignmentEntity(5, 12)
            )
        )
    }
}
