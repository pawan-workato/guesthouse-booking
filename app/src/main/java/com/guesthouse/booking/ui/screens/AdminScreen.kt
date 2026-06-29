package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.viewmodel.AdminViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onDismissConflict: (Long) -> Unit = {},
    onEditBooking: (Long) -> Unit = {}
) {
    val bookings by viewModel.bookingsWithDetails.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showCancelled by viewModel.showCancelled.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    LaunchedEffect(actionMessage, actionError) {
        if (actionMessage != null || actionError != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissActionFeedback()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bookings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "${bookings.size} ${if (showCancelled) "total" else "active"} bookings for your assigned properties",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show cancelled", modifier = Modifier.weight(1f))
            Switch(checked = showCancelled, onCheckedChange = viewModel::setShowCancelled)
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search guest, property, room, ref, or status") },
            singleLine = true
        )
        actionMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        }
        actionError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (bookings.isEmpty()) {
            Text("No bookings yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookings, key = { it.booking.id }) { item ->
                    val b = item.booking
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(item.roomName, fontWeight = FontWeight.SemiBold)
                            if (b.bookingReference.isNotBlank()) Text("Ref: ${b.bookingReference}")
                            Text(b.guestName)
                            if (b.guestPhone.isNotBlank()) Text(b.guestPhone)
                            if (b.guestEmail.isNotBlank()) Text(b.guestEmail)
                            Text("${LocalDate.ofEpochDay(b.checkInEpochDay).format(formatter)} → ${LocalDate.ofEpochDay(b.checkOutEpochDay).format(formatter)}")
                            Text("Booking: ${b.status}", color = bookingStatusColor(b.status))
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
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (b.status == BookingStatus.CONFIRMED.name) {
                                        TextButton(onClick = { onEditBooking(b.id) }) { Text("Edit") }
                                        TextButton(onClick = { viewModel.checkIn(b.id) }) { Text("Check in") }
                                        TextButton(onClick = { viewModel.cancelBooking(b.id) }) { Text("Cancel") }
                                    } else if (b.status == BookingStatus.CHECKED_IN.name) {
                                        TextButton(onClick = { viewModel.checkOut(b.id) }) { Text("Check out") }
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

@Composable
private fun bookingStatusColor(status: String): Color = when (status) {
    BookingStatus.CONFIRMED.name -> MaterialTheme.colorScheme.primary
    BookingStatus.CHECKED_IN.name -> MaterialTheme.colorScheme.tertiary
    BookingStatus.CHECKED_OUT.name -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}
