package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.ui.components.AvailabilityCalendar
import com.guesthouse.booking.ui.components.bookedDaysFromRanges
import com.guesthouse.booking.viewmodel.BookingViewModel
import java.util.Locale

@Composable
fun RoomDetailScreen(roomId: Long, viewModel: BookingViewModel, onBookNow: () -> Unit) {
    val rooms by viewModel.rooms.collectAsState()
    val room = rooms.find { it.id == roomId }
    val bookings by viewModel.observeRoomBookings(roomId).collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (room == null) { Text("Room not found"); return@Column }
        Text(room.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(room.description, Modifier.padding(vertical = 8.dp))
        Text("$${String.format(Locale.US, "%.0f", room.pricePerNight)}/night · up to ${room.capacity} guests",
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
        Text("Availability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        AvailabilityCalendar(
            bookedEpochDays = bookedDaysFromRanges(bookings.map { it.checkInEpochDay to it.checkOutEpochDay }),
            selectedCheckIn = null, selectedCheckOut = null, onDateSelected = {},
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        Button(onClick = onBookNow, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Book this room")
        }
    }
}
