package com.guesthouse.booking.viewmodel

import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType

object BookingSearchFilters {
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
