package com.guesthouse.booking.data.local

import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType

object RoomTypeSummary {
    fun countByType(rooms: List<RoomEntity>): Map<RoomType, Int> =
        rooms.groupingBy { RoomType.fromStored(it.roomType) }.eachCount()

    fun formatBreakdown(rooms: List<RoomEntity>): String {
        val counts = countByType(rooms)
        if (counts.isEmpty()) return ""
        return RoomType.entries
            .mapNotNull { type -> counts[type]?.takeIf { it > 0 }?.let { count -> "$count ${type.displayLabel()}" } }
            .joinToString(" · ")
    }
}
