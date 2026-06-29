package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.BlockDateWithDetails
import com.guesthouse.booking.viewmodel.BookingWithDetails
import com.guesthouse.booking.viewmodel.SyncViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    viewModel: SyncViewModel,
    isFirebaseConfigured: Boolean,
    onBack: () -> Unit,
    onOpenBooking: (Long) -> Unit,
    onDismissConflict: (Long) -> Unit
) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lastSyncMs by viewModel.lastSyncEpochMs.collectAsStateWithLifecycle()
    val pendingBookings by viewModel.pendingBookingsWithDetails.collectAsStateWithLifecycle()
    val pendingBlocks by viewModel.pendingBlocksWithDetails.collectAsStateWithLifecycle()
    val conflictBookings by viewModel.conflictBookingsWithDetails.collectAsStateWithLifecycle()
    val blockConflicts by viewModel.blockConflictsWithDetails.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val lastSyncLabel = remember(lastSyncMs) { formatLastSync(lastSyncMs) }

    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message == null && uiState.error == null) return@LaunchedEffect
        kotlinx.coroutines.delay(4000)
        viewModel.clearMessage()
    }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("Sync status") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Text(
                                if (isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("Last sync: $lastSyncLabel", style = MaterialTheme.typography.bodyMedium)
                        if (!isFirebaseConfigured) {
                            Text(
                                "Firebase is not configured — changes stay on this device only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.clearMessage(); viewModel.syncNow() },
                            enabled = isOnline && !uiState.isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Syncing…")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Sync now")
                            }
                        }
                        uiState.message?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                        uiState.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (pendingBookings.isEmpty() && pendingBlocks.isEmpty() &&
                conflictBookings.isEmpty() && blockConflicts.isEmpty()
            ) {
                item {
                    Text(
                        "Everything is synced for your properties.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (pendingBookings.isNotEmpty() || pendingBlocks.isNotEmpty()) {
                item {
                    Text("Pending upload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(pendingBookings, key = { "booking-${it.booking.id}" }) { item ->
                    PendingBookingCard(item, dateFormatter)
                }
                items(pendingBlocks, key = { "block-${it.block.id}" }) { item ->
                    PendingBlockCard(item, dateFormatter)
                }
            }

            if (conflictBookings.isNotEmpty() || blockConflicts.isNotEmpty()) {
                item {
                    Text(
                        "Conflicts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(conflictBookings, key = { "conflict-${it.booking.id}" }) { item ->
                    ConflictBookingCard(
                        item = item,
                        dateFormatter = dateFormatter,
                        onOpen = { onOpenBooking(item.booking.id) },
                        onDismiss = { onDismissConflict(item.booking.id) }
                    )
                }
                items(blockConflicts, key = { "block-conflict-${it.block.id}" }) { item ->
                    BlockConflictCard(item, dateFormatter)
                }
            }
        }
    }
}

@Composable
private fun PendingBookingCard(item: BookingWithDetails, formatter: DateTimeFormatter) {
    val b = item.booking
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Booking", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(item.propertyName, fontWeight = FontWeight.SemiBold)
            Text(item.roomName)
            if (b.bookingReference.isNotBlank()) Text("Ref: ${b.bookingReference}")
            Text(b.guestName)
            Text(
                "${LocalDate.ofEpochDay(b.checkInEpochDay).format(formatter)} → " +
                    LocalDate.ofEpochDay(b.checkOutEpochDay).format(formatter)
            )
            Text("Waiting to upload", color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun PendingBlockCard(item: BlockDateWithDetails, formatter: DateTimeFormatter) {
    val block = item.block
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Block dates", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(item.propertyName, fontWeight = FontWeight.SemiBold)
            Text(item.roomName)
            Text(
                "${LocalDate.ofEpochDay(block.startEpochDay).format(formatter)} → " +
                    LocalDate.ofEpochDay(block.endEpochDay).format(formatter)
            )
            if (block.reason.isNotBlank()) Text(block.reason)
            Text(
                if (block.markedForDeletion) "Pending removal" else "Waiting to upload",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun ConflictBookingCard(
    item: BookingWithDetails,
    dateFormatter: DateTimeFormatter,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val b = item.booking
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.propertyName, fontWeight = FontWeight.SemiBold)
            Text(item.roomName)
            Text(b.guestName)
            Text(
                "${LocalDate.ofEpochDay(b.checkInEpochDay).format(dateFormatter)} → " +
                    LocalDate.ofEpochDay(b.checkOutEpochDay).format(dateFormatter)
            )
            Text(
                "Another booking blocked these dates during sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpen) { Text("View booking") }
                TextButton(onClick = onDismiss) { Text("Cancel this booking") }
            }
        }
    }
}

@Composable
private fun BlockConflictCard(item: BlockDateWithDetails, formatter: DateTimeFormatter) {
    val block = item.block
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.propertyName, fontWeight = FontWeight.SemiBold)
            Text(item.roomName)
            Text(
                "${LocalDate.ofEpochDay(block.startEpochDay).format(formatter)} → " +
                    LocalDate.ofEpochDay(block.endEpochDay).format(formatter)
            )
            Text(
                "Block overlaps another block on the server. Edit or remove from room detail.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatLastSync(epochMs: Long): String {
    if (epochMs <= 0L) return "Never"
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
}
