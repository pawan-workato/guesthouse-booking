package com.guesthouse.booking.data.repository

import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.SyncStatus

data class PropertyRevenueStats(
    val propertyId: Long,
    val propertyName: String,
    val bookingCount: Int,
    val totalRevenue: Double
)

object RevenueCalculator {

    fun revenueForBooking(booking: BookingEntity, pricePerNight: Double): Double {
        val nights = (booking.checkOutEpochDay - booking.checkInEpochDay).coerceAtLeast(0)
        return nights * pricePerNight
    }

    fun isEligible(booking: BookingEntity): Boolean =
        booking.status != BookingStatus.CANCELLED.name &&
            booking.syncStatus != SyncStatus.CONFLICT.name

    fun overlapsDate(booking: BookingEntity, epochDay: Long): Boolean =
        booking.checkInEpochDay <= epochDay && booking.checkOutEpochDay > epochDay

    fun statsForProperties(
        properties: List<Pair<Long, String>>,
        bookings: List<BookingEntity>,
        roomsById: Map<Long, RoomEntity>,
        epochDay: Long
    ): List<PropertyRevenueStats> {
        val eligible = bookings.filter { isEligible(it) && overlapsDate(it, epochDay) }
        return properties.map { (propertyId, propertyName) ->
            val propertyBookings = eligible.filter { it.propertyId == propertyId }
            val total = propertyBookings.sumOf { booking ->
                val price = roomsById[booking.roomId]?.pricePerNight ?: 0.0
                revenueForBooking(booking, price)
            }
            PropertyRevenueStats(
                propertyId = propertyId,
                propertyName = propertyName,
                bookingCount = propertyBookings.size,
                totalRevenue = total
            )
        }
    }
}
