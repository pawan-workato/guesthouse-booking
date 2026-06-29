package com.guesthouse.booking.viewmodel

import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType

object BookingSearchFilters {
    fun filterBookingsWithDetails(
        bookings: List<BookingWithDetails>,
        query: String
    ): List<BookingWithDetails> {
        val q = query.trim()
        if (q.isBlank()) return bookings
        return bookings.filter { item ->
            val b = item.booking
            b.guestName.contains(q, ignoreCase = true) ||
                b.guestEmail.contains(q, ignoreCase = true) ||
                b.guestPhone.contains(q, ignoreCase = true) ||
                item.propertyName.contains(q, ignoreCase = true) ||
                item.roomName.contains(q, ignoreCase = true) ||
                b.bookingReference.contains(q, ignoreCase = true) ||
                b.status.contains(q, ignoreCase = true) ||
                b.status.replace('_', ' ').contains(q, ignoreCase = true)
        }
    }

    fun filterProperties(properties: List<PropertyEntity>, query: String): List<PropertyEntity> {
        val q = query.trim()
        if (q.isBlank()) return properties
        return properties.filter { property ->
            property.name.contains(q, ignoreCase = true) ||
                property.region.contains(q, ignoreCase = true) ||
                property.address.contains(q, ignoreCase = true)
        }
    }

    fun filterRooms(
        rooms: List<RoomEntity>,
        query: String,
        roomTypeFilter: RoomType?
    ): List<RoomEntity> {
        val q = query.trim()
        return rooms.filter { room ->
            val type = RoomType.fromStored(room.roomType)
            val matchesType = roomTypeFilter == null || type == roomTypeFilter
            val matchesQuery = q.isBlank() ||
                room.name.contains(q, ignoreCase = true) ||
                room.description.contains(q, ignoreCase = true) ||
                type.displayLabel().contains(q, ignoreCase = true) ||
                type.name.contains(q, ignoreCase = true)
            matchesType && matchesQuery
        }
    }
}
