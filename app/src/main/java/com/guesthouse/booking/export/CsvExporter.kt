package com.guesthouse.booking.export

import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.data.local.entities.RoomEntity
import java.time.LocalDate

object CsvExporter {

    fun exportBookings(
        bookings: List<BookingEntity>,
        properties: Map<Long, PropertyEntity>,
        rooms: Map<Long, RoomEntity>
    ): String {
        val header = listOf(
            "Reference",
            "Property",
            "Room",
            "Guest",
            "Email",
            "Phone",
            "Check-in",
            "Check-out",
            "Status",
            "Sync status"
        ).joinToString(",")
        val rows = bookings.map { booking ->
            listOf(
                booking.bookingReference,
                properties[booking.propertyId]?.name.orEmpty(),
                rooms[booking.roomId]?.name.orEmpty(),
                booking.guestName,
                booking.guestEmail,
                booking.guestPhone,
                formatEpochDay(booking.checkInEpochDay),
                formatEpochDay(booking.checkOutEpochDay),
                booking.status,
                booking.syncStatus
            ).joinToString(",") { escapeCsv(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    fun exportGuests(guests: List<GuestEntity>): String {
        val header = listOf("Name", "Email", "Phone", "Notes", "Active").joinToString(",")
        val rows = guests.map { guest ->
            listOf(
                guest.name,
                guest.email,
                guest.phone,
                guest.notes,
                if (guest.isActive) "yes" else "no"
            ).joinToString(",") { escapeCsv(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun formatEpochDay(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).toString()

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
