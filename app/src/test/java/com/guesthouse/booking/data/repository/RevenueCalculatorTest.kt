package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueCalculatorTest {

    private val room = RoomEntity(
        id = 1L,
        propertyId = 10L,
        name = "Double",
        description = "",
        pricePerNight = 100.0,
        capacity = 2
    )

    private fun booking(
        checkIn: Long,
        checkOut: Long,
        status: String = BookingStatus.CONFIRMED.name,
        syncStatus: String = SyncStatus.SYNCED.name
    ) = BookingEntity(
        id = 1L,
        propertyId = 10L,
        roomId = 1L,
        guestName = "Guest",
        guestEmail = "",
        checkInEpochDay = checkIn,
        checkOutEpochDay = checkOut,
        status = status,
        syncStatus = syncStatus
    )

    @Test
    fun revenueForBooking_multipliesNightsByPrice() {
        val revenue = RevenueCalculator.revenueForBooking(booking(100, 103), 120.0)
        assertEquals(360.0, revenue, 0.001)
    }

    @Test
    fun isEligible_rejectsCancelledAndConflict() {
        assertFalse(RevenueCalculator.isEligible(booking(1, 3, status = BookingStatus.CANCELLED.name)))
        assertFalse(RevenueCalculator.isEligible(booking(1, 3, syncStatus = SyncStatus.CONFLICT.name)))
        assertTrue(RevenueCalculator.isEligible(booking(1, 3)))
    }

    @Test
    fun statsForProperties_sumsOverlappingStays() {
        val bookings = listOf(
            booking(checkIn = 10, checkOut = 13),
            booking(checkIn = 9, checkOut = 11, status = BookingStatus.CANCELLED.name)
        )
        val stats = RevenueCalculator.statsForProperties(
            properties = listOf(10L to "Lodge"),
            bookings = bookings,
            roomsById = mapOf(1L to room),
            epochDay = 10L
        )
        assertEquals(1, stats.single().bookingCount)
        assertEquals(300.0, stats.single().totalRevenue, 0.001)
    }
}
