package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.viewmodel.AdminViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AdminScreen(viewModel: AdminViewModel) {
    val bookings by viewModel.bookingsWithRooms.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Admin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Manage all bookings", color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp))

        if (bookings.isEmpty()) {
            Text("No bookings yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookings, key = { it.booking.id }) { item ->
                    val b = item.booking
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.roomName, fontWeight = FontWeight.SemiBold)
                            Text("${b.guestName} · ${b.guestEmail}")
                            Text("${LocalDate.ofEpochDay(b.checkInEpochDay).format(formatter)} → ${LocalDate.ofEpochDay(b.checkOutEpochDay).format(formatter)}")
                            Text("Status: ${b.status}", color = if (b.status == BookingStatus.CONFIRMED.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            if (b.status == BookingStatus.CONFIRMED.name) {
                                TextButton(onClick = { viewModel.cancelBooking(b.id) }) {
                                    Text("Cancel booking")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
