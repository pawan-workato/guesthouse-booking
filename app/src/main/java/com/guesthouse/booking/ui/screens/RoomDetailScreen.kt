package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.ui.components.AvailabilityCalendar
import com.guesthouse.booking.ui.components.bookedDaysFromRanges
import com.guesthouse.booking.viewmodel.BookingViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: Long,
    viewModel: BookingViewModel,
    onBack: () -> Unit,
    onBookNow: (propertyId: Long, roomId: Long) -> Unit
) {
    val room by viewModel.room(roomId).collectAsState()
    val bookings by viewModel.observeRoomBookings(roomId).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room?.name ?: "Room") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (room == null) {
                Text("Room not found")
                return@Column
            }
            val r = room!!
            Text(r.description, Modifier.padding(bottom = 8.dp))
            Text(
                "$${String.format(Locale.US, "%.0f", r.pricePerNight)}/night · up to ${r.capacity} guests",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text("Availability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            AvailabilityCalendar(
                bookedEpochDays = bookedDaysFromRanges(bookings.map { it.checkInEpochDay to it.checkOutEpochDay }),
                selectedCheckIn = null,
                selectedCheckOut = null,
                onDateSelected = {},
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Button(
                onClick = { onBookNow(r.propertyId, r.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Book for guest")
            }
        }
    }
}
