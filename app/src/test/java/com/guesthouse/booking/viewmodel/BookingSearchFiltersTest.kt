package com.guesthouse.booking.viewmodel

import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingSearchFiltersTest {

    private val properties = listOf(
        PropertyEntity(1L, "Mountain Lodge", "Denver, CO", "Mountain West", "15:00", "11:00"),
        PropertyEntity(2L, "Coastal Inn", "San Diego, CA", "Pacific", "15:00", "11:00")
    )

    private val rooms = listOf(
        RoomEntity(1L, 1L, "Summit Double", "View", 120.0, 2, RoomType.DOUBLE.name),
        RoomEntity(2L, 1L, "Pine Single", "Quiet", 80.0, 1, RoomType.SINGLE.name)
    )

    @Test
    fun filterProperties_matchesNameRegionOrAddress() {
        assertEquals(1, BookingSearchFilters.filterProperties(properties, "coastal").size)
        assertEquals(1, BookingSearchFilters.filterProperties(properties, "denver").size)
        assertEquals(2, BookingSearchFilters.filterProperties(properties, "").size)
    }

    @Test
    fun filterRooms_matchesQueryAndRoomType() {
        assertEquals(1, BookingSearchFilters.filterRooms(rooms, "single", null).size)
        assertEquals(1, BookingSearchFilters.filterRooms(rooms, "", RoomType.DOUBLE).size)
        assertEquals(0, BookingSearchFilters.filterRooms(rooms, "suite", null).size)
    }
}
