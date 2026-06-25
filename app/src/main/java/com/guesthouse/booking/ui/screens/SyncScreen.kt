package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.BookingEntity
import com.guesthouse.booking.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(viewModel: SyncViewModel) {
    val isOnline by viewModel.isOnline.collectAsState()
    val lastSync by viewModel.lastSyncEpochMs.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sync", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = {},
                label = { Text(if (isOnline) "Online" else "Offline") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            )
            if (lastSync > 0L) {
                val formatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(lastSync))
                Text("Last sync: $formatted", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Never synced", style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(
            onClick = { viewModel.clearMessage(); viewModel.syncNow() },
            enabled = isOnline && !uiState.isSyncing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isSyncing) "Syncing..." else "Sync now")
        }

        uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        if (pending.isNotEmpty()) {
            Text("Pending (${pending.size})", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            pending.forEach { BookingSyncRow(it, dateFormatter) }
        }

        if (conflicts.isNotEmpty()) {
            Text("Conflicts (${conflicts.size})", fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(conflicts, key = { it.id }) { booking ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            BookingSyncRow(booking, dateFormatter)
                            Text("Another booking blocked these dates during sync.", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { viewModel.dismissConflict(booking.id) }) {
                                Text("Cancel this booking")
                            }
                        }
                    }
                }
            }
        }

        if (pending.isEmpty() && conflicts.isEmpty()) {
            Text("All bookings are synced.", modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun BookingSyncRow(booking: BookingEntity, formatter: DateTimeFormatter) {
    Text("${booking.bookingReference} · ${booking.guestName}")
    Text(
        "${LocalDate.ofEpochDay(booking.checkInEpochDay).format(formatter)} → " +
            LocalDate.ofEpochDay(booking.checkOutEpochDay).format(formatter),
        style = MaterialTheme.typography.bodySmall
    )
}
