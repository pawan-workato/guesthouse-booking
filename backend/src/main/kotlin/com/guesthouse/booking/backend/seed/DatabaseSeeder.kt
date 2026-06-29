package com.guesthouse.booking.backend.seed

import com.guesthouse.booking.backend.auth.PasswordHasher
import com.guesthouse.booking.backend.db.Guests
import com.guesthouse.booking.backend.db.Properties
import com.guesthouse.booking.backend.db.Rooms
import com.guesthouse.booking.backend.db.Staff
import com.guesthouse.booking.backend.db.StaffPropertyAssignments
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseSeeder {
    fun seedIfEmpty() {
        transaction {
            if (Properties.selectAll().count() > 0) return@transaction
            seedProperties()
            seedRooms()
            seedGuests()
            seedStaff()
        }
    }

    private fun seedProperties() {
        val rows = listOf(
            PropertySeed(1, "Hill View Guesthouse", "142 Ridge Rd, Aspen, CO", "Mountain West"),
            PropertySeed(2, "Riverside Lodge", "88 River Walk, Portland, OR", "Pacific NW"),
            PropertySeed(3, "Cedar Inn", "19 Cedar Ln, Bozeman, MT", "Mountain West"),
            PropertySeed(4, "Harbor House", "5 Pier St, Monterey, CA", "Coastal"),
            PropertySeed(5, "Maple Retreat", "301 Maple Ave, Burlington, VT", "Northeast"),
            PropertySeed(6, "Sunstone Villa", "44 Desert Dr, Sedona, AZ", "Southwest"),
            PropertySeed(7, "Pinecrest Lodge", "77 Pine Rd, Jackson, WY", "Mountain West"),
            PropertySeed(8, "Lakeside Haven", "12 Lakeview Dr, Traverse City, MI", "Midwest"),
            PropertySeed(9, "Desert Bloom Inn", "210 Cactus Way, Santa Fe, NM", "Southwest"),
            PropertySeed(10, "Oak & Ivy Guesthouse", "56 Oak St, Asheville, NC", "Southeast"),
            PropertySeed(11, "Summit Stay", "901 Summit Blvd, Denver, CO", "Mountain West"),
            PropertySeed(12, "Meadowbrook Cottage", "3 Meadow Ln, Madison, WI", "Midwest")
        )
        for (row in rows) {
            Properties.insert {
                it[id] = EntityID(row.id, Properties)
                it[name] = row.name
                it[address] = row.address
                it[region] = row.region
            }
        }
    }


    private fun inferRoomType(name: String, capacity: Int): String {
        val lower = name.lowercase()
        return when {
            "single" in lower -> "SINGLE"
            "double" in lower -> "DOUBLE"
            "suite" in lower -> "SUITE"
            "family" in lower || "cottage" in lower -> "FAMILY"
            "den" in lower && capacity >= 4 -> "FAMILY"
            capacity <= 1 -> "SINGLE"
            capacity >= 4 -> "FAMILY"
            else -> "DOUBLE"
        }
    }

    private data class PropertySeed(val id: Long, val name: String, val address: String, val region: String)

    private fun seedRooms() {
        val rooms = listOf(
            RoomSeed(1, "Garden Suite", "Ground-floor patio and garden views.", 89.0, 2),
            RoomSeed(1, "Loft Room", "Upper-floor skylight and workspace.", 75.0, 2),
            RoomSeed(1, "Family Room", "Two queen beds for families.", 120.0, 4),
            RoomSeed(2, "River View", "Balcony overlooking the river.", 95.0, 2),
            RoomSeed(2, "Studio", "Compact studio with kitchenette.", 70.0, 2),
            RoomSeed(2, "Suite", "Separate living area and bedroom.", 130.0, 3),
            RoomSeed(3, "Cedar Double", "Warm wood finishes, mountain view.", 80.0, 2),
            RoomSeed(3, "Cozy Single", "Ideal for solo travelers.", 55.0, 1),
            RoomSeed(4, "Harbor King", "King bed with ocean glimpse.", 110.0, 2),
            RoomSeed(4, "Anchor Room", "Nautical theme, queen bed.", 85.0, 2),
            RoomSeed(4, "Captain's Suite", "Corner suite with bay windows.", 145.0, 4),
            RoomSeed(5, "Maple Standard", "Classic room with maple grove view.", 78.0, 2),
            RoomSeed(5, "Autumn Suite", "Spacious suite, fireplace.", 115.0, 3),
            RoomSeed(6, "Adobe Room", "Southwestern adobe styling.", 92.0, 2),
            RoomSeed(6, "Terrace Double", "Private terrace, red rock views.", 105.0, 2),
            RoomSeed(6, "Poolside", "Steps from the courtyard pool.", 98.0, 2),
            RoomSeed(7, "Pine Standard", "Forest-facing double room.", 88.0, 2),
            RoomSeed(7, "Bear Den", "Rustic lodge feel, two beds.", 100.0, 4),
            RoomSeed(8, "Lakeview Double", "Direct lake views.", 90.0, 2),
            RoomSeed(8, "Dock Room", "Near the private dock.", 82.0, 2),
            RoomSeed(8, "Family Cottage", "Two-bedroom cottage unit.", 155.0, 6),
            RoomSeed(9, "Bloom Single", "Courtyard garden access.", 65.0, 1),
            RoomSeed(9, "Adobe Double", "Traditional pueblo design.", 85.0, 2),
            RoomSeed(10, "Ivy Room", "Garden-level, wheelchair accessible.", 79.0, 2),
            RoomSeed(10, "Oak Suite", "Top floor with mountain views.", 112.0, 3),
            RoomSeed(10, "Carriage House", "Detached unit with kitchen.", 135.0, 4),
            RoomSeed(11, "Summit Double", "City and mountain skyline.", 86.0, 2),
            RoomSeed(11, "Alpine Room", "Quiet rear-facing room.", 72.0, 2),
            RoomSeed(12, "Meadow Double", "Pasture views, ground floor.", 74.0, 2),
            RoomSeed(12, "Brook Suite", "Stream-side suite with sitting area.", 99.0, 3)
        )
        for (room in rooms) {
            Rooms.insert {
                it[propertyId] = EntityID(room.propertyId.toLong(), Properties)
                it[name] = room.name
                it[description] = room.description
                it[pricePerNight] = room.pricePerNight
                it[capacity] = room.capacity
                it[roomType] = inferRoomType(room.name, room.capacity)
            }
        }
    }

    private data class RoomSeed(
        val propertyId: Int,
        val name: String,
        val description: String,
        val pricePerNight: Double,
        val capacity: Int
    )

    private fun seedGuests() {
        val now = System.currentTimeMillis()
        val guests = listOf(
            GuestSeed(1, "Maria Chen", "maria.chen@example.com", "+1 555-0101", "Prefers ground-floor rooms"),
            GuestSeed(2, "James O'Brien", "j.obrien@example.com", "+1 555-0102", "Late check-in often"),
            GuestSeed(3, "Priya Sharma", "priya.sh@example.com", "+1 555-0103", ""),
            GuestSeed(4, "Robert Kim", "r.kim@example.com", "+1 555-0104", "Traveling with service dog"),
            GuestSeed(5, "Elena Vasquez", "elena.v@example.com", "+1 555-0105", "Allergic to down pillows")
        )
        for (guest in guests) {
            Guests.insert {
                it[id] = EntityID(guest.id, Guests)
                it[name] = guest.name
                it[email] = guest.email
                it[phone] = guest.phone
                it[notes] = guest.notes
                it[createdAtEpochMs] = now
                it[updatedAtEpochMs] = now
            }
        }
    }

    private data class GuestSeed(
        val id: Long,
        val name: String,
        val email: String,
        val phone: String,
        val notes: String
    )

    private fun seedStaff() {
        if (Staff.selectAll().count() > 0) return

        val adminHash = PasswordHasher.hash("admin123")
        val managerHash = PasswordHasher.hash("manager123")

        val staffRows = listOf(
            StaffSeed(1, "admin@chain.com", adminHash, "Chain Admin", "CHAIN_ADMIN"),
            StaffSeed(2, "manager.mountain@chain.com", managerHash, "Alex Mountain", "PROPERTY_MANAGER"),
            StaffSeed(3, "manager.coastal@chain.com", managerHash, "Sam Coastal", "PROPERTY_MANAGER"),
            StaffSeed(4, "manager.southwest@chain.com", managerHash, "Jordan Southwest", "PROPERTY_MANAGER"),
            StaffSeed(5, "manager.east@chain.com", managerHash, "Taylor East", "PROPERTY_MANAGER")
        )
        for (row in staffRows) {
            Staff.insert {
                it[id] = EntityID(row.id, Staff)
                it[email] = row.email
                it[passwordHash] = row.passwordHash
                it[displayName] = row.displayName
                it[role] = row.role
            }
        }

        val assignments = listOf(
            2L to 1L, 2L to 3L, 2L to 7L, 2L to 11L,
            3L to 2L, 3L to 4L, 3L to 8L,
            4L to 6L, 4L to 9L,
            5L to 5L, 5L to 10L, 5L to 12L
        )
        for ((staffId, propertyId) in assignments) {
            StaffPropertyAssignments.insert {
                it[StaffPropertyAssignments.staffId] = EntityID(staffId, Staff)
                it[StaffPropertyAssignments.propertyId] = EntityID(propertyId, Properties)
            }
        }
    }

    private data class StaffSeed(
        val id: Long,
        val email: String,
        val passwordHash: String,
        val displayName: String,
        val role: String
    )
}
