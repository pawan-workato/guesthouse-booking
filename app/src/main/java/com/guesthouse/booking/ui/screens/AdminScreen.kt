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
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.viewmodel.AdminViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onDismissConflict: (Long) -> Unit = {},
    onEditBooking: (Long) -> Unit = {}
) {
    val bookings by viewModel.bookingsWithDetails.collectAsState()
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bookings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Your assigned properties",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (bookings.isEmpty()) {
            Text("No bookings yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookings, key = { it.booking.id }) { item ->
                    val b = item.booking
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(item.roomName, fontWeight = FontWeight.SemiBold)
                            if (b.bookingReference.isNotBlank()) Text("Ref: ${b.bookingReference}")
                            Text(b.guestName)
                            if (b.guestPhone.isNotBlank()) Text(b.guestPhone)
                            if (b.guestEmail.isNotBlank()) Text(b.guestEmail)
                            Text("${LocalDate.ofEpochDay(b.checkInEpochDay).format(formatter)} → ${LocalDate.ofEpochDay(b.checkOutEpochDay).format(formatter)}")
                            Text("Booking: ${b.status}", color = if (b.status == BookingStatus.CONFIRMED.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(
                                "Sync: ${b.syncStatus.replace('_', ' ')}",
                                color = when (b.syncStatus) {
                                    SyncStatus.CONFLICT.name -> MaterialTheme.colorScheme.error
                                    SyncStatus.PENDING_SYNC.name -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (b.syncStatus == SyncStatus.CONFLICT.name) {
                                Text(
                                    "Another booking blocked these dates during sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                TextButton(onClick = { onDismissConflict(b.id) }) {
                                    Text("Cancel this booking")
                                }
                            } else if (b.status == BookingStatus.CONFIRMED.name) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onEditBooking(b.id) }) {
                                        Text("Edit booking")
                                    }
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
}
