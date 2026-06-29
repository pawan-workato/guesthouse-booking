package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.SyncStatus
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.BookingDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    bookingId: Long,
    viewModel: BookingDetailViewModel,
    onBack: () -> Unit,
    onEditBooking: (Long) -> Unit,
    onOpenGuest: (Long) -> Unit,
    onBookAgain: (Long?) -> Unit,
    onDismissConflict: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    LaunchedEffect(bookingId) { viewModel.loadBooking(bookingId) }
    LaunchedEffect(state.actionMessage, state.actionError, state.extendMessage, state.extendError) {
        if (state.actionMessage != null || state.actionError != null ||
            state.extendMessage != null || state.extendError != null
        ) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissActionFeedback()
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val booking = state.booking
    if (state.accessDenied || booking == null) {
        GlassScaffold(
            topBar = {
                GlassTopAppBar(
                    title = { Text("Booking") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Text("Booking not found or access denied.", Modifier.padding(padding).padding(16.dp))
        }
        return
    }

    var showExtendPicker by remember { mutableStateOf(false) }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("Booking details") },
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
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(state.roomName, fontWeight = FontWeight.SemiBold)
                    Text(booking.guestName, style = MaterialTheme.typography.titleMedium)
                    if (booking.guestEmail.isNotBlank()) Text(booking.guestEmail)
                    if (booking.guestPhone.isNotBlank()) Text(booking.guestPhone)
                    Text(
                        "${LocalDate.ofEpochDay(booking.checkInEpochDay).format(formatter)} → " +
                            LocalDate.ofEpochDay(booking.checkOutEpochDay).format(formatter)
                    )
                    Text("Status: ${booking.status.replace('_', ' ')}", color = bookingStatusColor(booking.status))
                    if (booking.bookingReference.isNotBlank()) Text("Ref: ${booking.bookingReference}")
                    Text(
                        "Sync: ${booking.syncStatus.replace('_', ' ')}",
                        color = when (booking.syncStatus) {
                            SyncStatus.CONFLICT.name -> MaterialTheme.colorScheme.error
                            SyncStatus.PENDING_SYNC.name -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (state.hasConflict) {
                        Text(
                            "Another booking blocked these dates during sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.extendMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.extendError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            booking.guestId?.let { guestId ->
                OutlinedButton(onClick = { onOpenGuest(guestId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View guest profile")
                }
            }

            OutlinedButton(
                onClick = { onBookAgain(booking.guestId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book again")
            }

            if (state.hasConflict) {
                TextButton(onClick = { onDismissConflict(booking.id) }) {
                    Text("Cancel this booking")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.canEdit) {
                        TextButton(onClick = { onEditBooking(booking.id) }) { Text("Edit") }
                        TextButton(onClick = { viewModel.checkIn() }) { Text("Check in") }
                        TextButton(onClick = { viewModel.cancelBooking() }) { Text("Cancel") }
                    } else if (state.canCheckOut) {
                        TextButton(onClick = { viewModel.checkOut() }) { Text("Check out") }
                    }
                }
            }

            if (state.canExtend && !state.hasConflict) {
                Text("Extend stay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { nights ->
                        OutlinedButton(
                            onClick = { viewModel.extendCheckout(nights) },
                            enabled = !state.isExtending
                        ) {
                            Text("+$nights night${if (nights > 1) "s" else ""}")
                        }
                    }
                    OutlinedButton(onClick = { showExtendPicker = true }, enabled = !state.isExtending) {
                        Text("Pick date")
                    }
                }
            }
        }
    }

    if (showExtendPicker) {
        val minDate = booking.checkOutEpochDay + 1
        var picked by remember { mutableLongStateOf(minDate) }
        AlertDialog(
            onDismissRequest = { showExtendPicker = false },
            title = { Text("New check-out date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current check-out: ${LocalDate.ofEpochDay(booking.checkOutEpochDay).format(formatter)}")
                    OutlinedTextField(
                        value = LocalDate.ofEpochDay(picked).format(formatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { picked = minDate }) { Text("Next day") }
                        OutlinedButton(onClick = { picked += 1 }) { Text("+1") }
                        OutlinedButton(onClick = { picked += 7 }) { Text("+7 days") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.extendCheckoutTo(picked)
                    showExtendPicker = false
                }) { Text("Extend") }
            },
            dismissButton = {
                TextButton(onClick = { showExtendPicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun bookingStatusColor(status: String): Color = when (status) {
    BookingStatus.CONFIRMED.name -> MaterialTheme.colorScheme.primary
    BookingStatus.CHECKED_IN.name -> MaterialTheme.colorScheme.tertiary
    BookingStatus.CHECKED_OUT.name -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}
