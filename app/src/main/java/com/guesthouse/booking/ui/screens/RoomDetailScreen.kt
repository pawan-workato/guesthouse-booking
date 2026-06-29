package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guesthouse.booking.data.local.entities.BlockDateEntity
import com.guesthouse.booking.ui.components.AvailabilityCalendar
import com.guesthouse.booking.ui.components.bookedDaysFromRanges
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.glassTopAppBarColors
import com.guesthouse.booking.viewmodel.BookingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: Long,
    viewModel: BookingViewModel,
    canEditRoom: Boolean,
    onBack: () -> Unit,
    onBookNow: (Long, Long) -> Unit,
    onEditRoom: () -> Unit
) {
    val room by viewModel.room(roomId).collectAsState()
    val bookings by viewModel.observeRoomBookings(roomId).collectAsState()
    val blocks by viewModel.observeRoomBlocks(roomId).collectAsState()
    val blockUiState by viewModel.blockUiState.collectAsState()
    var showBlockDialog by remember { mutableStateOf(false) }
    LaunchedEffect(blockUiState.successMessage) { if (blockUiState.successMessage != null) { showBlockDialog = false; viewModel.clearBlockMessages() } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
        TopAppBar(
            colors = glassTopAppBarColors(),
            title = { Text(room?.name ?: "Room") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (canEditRoom) {
                    IconButton(onClick = onEditRoom) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit room")
                    }
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (room == null) { Text("Room not found"); return@Column }
            val r = room!!
            val bookedDays = bookedDaysFromRanges(bookings.map { it.checkInEpochDay to it.checkOutEpochDay })
            val blockedDays = bookedDaysFromRanges(blocks.map { it.startEpochDay to it.endEpochDay })
            val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
            Text(r.description, Modifier.padding(bottom = 8.dp))
            Text("$${String.format(Locale.US, "%.0f", r.pricePerNight)}/night · up to ${r.capacity} guests", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            Text("Availability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            AvailabilityCalendar(bookedEpochDays=bookedDays, blockedEpochDays=blockedDays, selectedCheckIn=null, selectedCheckOut=null, onDateSelected={}, modifier=Modifier.fillMaxWidth().padding(vertical=8.dp))
            blocks.forEach { BlockRow(it, fmt) { viewModel.removeBlock(it.id) } }
            blockUiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showBlockDialog = true }, modifier = Modifier.weight(1f)) { Text("Block dates") }
                Button(onClick = { onBookNow(r.propertyId, r.id) }, modifier = Modifier.weight(1f)) { Text("Book for guest") }
            }
        }
    }
    if (showBlockDialog) BlockDialog(roomId, viewModel, bookedDaysFromRanges(bookings.map { it.checkInEpochDay to it.checkOutEpochDay }), bookedDaysFromRanges(blocks.map { it.startEpochDay to it.endEpochDay }), blockUiState, { showBlockDialog = false; viewModel.clearBlockMessages() })
}

@Composable private fun BlockRow(block: BlockDateEntity, fmt: DateTimeFormatter, onUnblock: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("${LocalDate.ofEpochDay(block.startEpochDay).format(fmt)} – ${LocalDate.ofEpochDay(block.endEpochDay).format(fmt)}"); if (block.reason.isNotBlank()) Text(block.reason, style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = onUnblock) { Icon(Icons.Default.Close, contentDescription = "Unblock") }
        }
    }
}

@Composable private fun BlockDialog(roomId: Long, vm: BookingViewModel, booked: Set<Long>, blocked: Set<Long>, state: com.guesthouse.booking.viewmodel.BlockUiState, dismiss: () -> Unit) {
    var start by remember { mutableStateOf<Long?>(null) }; var end by remember { mutableStateOf<Long?>(null) }; var reason by remember { mutableStateOf("") }
    Dialog(onDismissRequest = dismiss) {
        GlassCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Block dates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                AvailabilityCalendar(bookedEpochDays = booked, blockedEpochDays = blocked, selectedCheckIn = start, selectedCheckOut = end, onDateSelected = { d -> when { start == null || end != null -> { start = d; end = null }; d <= start!! -> start = d; else -> end = d } }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text("Reason (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = dismiss) { Text("Cancel") }
                    Button(onClick = { if (start != null && end != null) vm.createBlock(roomId, start!!, end!!, reason) }, enabled = !state.isSubmitting && start != null && end != null) { Text("Block") }
                }
            }
        }
    }
}
