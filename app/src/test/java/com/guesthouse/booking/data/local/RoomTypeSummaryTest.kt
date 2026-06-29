package com.guesthouse.booking.data.local

import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomTypeSummaryTest {

    @Test
    fun countByType_groupsRoomsByStoredType() {
        val rooms = listOf(
            RoomEntity(1L, 1L, "A", "", 80.0, 2, RoomType.DOUBLE.name),
            RoomEntity(2L, 1L, "B", "", 55.0, 1, RoomType.SINGLE.name),
            RoomEntity(3L, 1L, "C", "", 120.0, 2, RoomType.DOUBLE.name),
            RoomEntity(4L, 1L, "D", "", 140.0, 3, RoomType.SUITE.name),
        )
        assertEquals(
            mapOf(RoomType.DOUBLE to 2, RoomType.SINGLE to 1, RoomType.SUITE to 1),
            RoomTypeSummary.countByType(rooms)
        )
    }

    @Test
    fun formatBreakdown_ordersByEnumAndOmitsZeroCounts() {
        val rooms = listOf(
            RoomEntity(1L, 1L, "Double", "", 80.0, 2, RoomType.DOUBLE.name),
            RoomEntity(2L, 1L, "Double 2", "", 85.0, 2, RoomType.DOUBLE.name),
            RoomEntity(3L, 1L, "Single", "", 55.0, 1, RoomType.SINGLE.name),
            RoomEntity(4L, 1L, "Suite", "", 140.0, 3, RoomType.SUITE.name),
        )
        assertEquals("1 Single · 2 Double · 1 Suite", RoomTypeSummary.formatBreakdown(rooms))
    }
}
