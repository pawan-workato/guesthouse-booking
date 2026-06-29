package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.AppDatabase
import com.guesthouse.booking.data.local.entities.PropertyEntity
import java.time.LocalDate

data class DayRevenueForecast(
    val epochDay: Long,
    val stats: List<PropertyRevenueStats>,
    val totalRevenue: Double
)

class ReportsRepository(
    private val database: AppDatabase
) {
    suspend fun getRevenueStats(
        properties: List<PropertyEntity>,
        epochDay: Long
    ): List<PropertyRevenueStats> {
        if (properties.isEmpty()) return emptyList()
        val propertyIds = properties.map { it.id }.toSet()
        val bookings = database.bookingDao().getOverlappingDateRange(epochDay, epochDay + 1)
            .filter { it.propertyId in propertyIds }
        val rooms = database.roomDao().getAll().associateBy { it.id }
        return RevenueCalculator.statsForProperties(
            properties = properties.map { it.id to it.name },
            bookings = bookings,
            roomsById = rooms,
            epochDay = epochDay
        )
    }

    suspend fun getAllNonCancelledBookings() =
        database.bookingDao().getAllNonCancelled()

    suspend fun getAllRooms() = database.roomDao().getAll()

    suspend fun getAllGuests() = database.guestDao().getAllActive()

    suspend fun getWeekAheadForecast(
        properties: List<PropertyEntity>,
        startEpochDay: Long = LocalDate.now().toEpochDay()
    ): List<DayRevenueForecast> {
        return (0 until 7).map { offset ->
            val day = startEpochDay + offset
            val stats = getRevenueStats(properties, day)
            DayRevenueForecast(
                epochDay = day,
                stats = stats,
                totalRevenue = stats.sumOf { it.totalRevenue }
            )
        }
    }
}
