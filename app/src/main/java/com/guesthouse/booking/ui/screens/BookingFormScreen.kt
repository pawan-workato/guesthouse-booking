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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(viewModel: BookingViewModel) {
    val rooms by viewModel.rooms.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var selectedRoomId by remember { mutableLongStateOf(rooms.firstOrNull()?.id ?: 0L) }
    var guestName by remember { mutableStateOf("") }
    var guestEmail by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf<Long?>(null) }
    var checkOut by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(rooms) {
        if (selectedRoomId == 0L && rooms.isNotEmpty()) selectedRoomId = rooms.first().id
    }

    val bookings by viewModel.observeRoomBookings(selectedRoomId).collectAsState()
    val bookedDays = bookedDaysFromRanges(bookings.map { it.checkInEpochDay to it.checkOutEpochDay })
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("New Booking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Select dates and enter guest details", color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp))

        if (rooms.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val selectedRoom = rooms.find { it.id == selectedRoomId }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedRoom?.name ?: "Select room",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Room") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    rooms.forEach { room ->
                        DropdownMenuItem(
                            text = { Text("${room.name} — $${room.pricePerNight.toInt()}/night") },
                            onClick = {
                                selectedRoomId = room.id
                                checkIn = null
                                checkOut = null
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        AvailabilityCalendar(
            bookedEpochDays = bookedDays,
            selectedCheckIn = checkIn,
            selectedCheckOut = checkOut,
            onDateSelected = { day ->
                when {
                    checkIn == null || (checkIn != null && checkOut != null) -> {
                        checkIn = day; checkOut = null
                    }
                    day <= checkIn!! -> checkIn = day
                    else -> checkOut = day
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )

        if (checkIn != null) {
            Text("Check-in: ${LocalDate.ofEpochDay(checkIn!!).format(formatter)}")
        }
        if (checkOut != null) {
            Text("Check-out: ${LocalDate.ofEpochDay(checkOut!!).format(formatter)}")
        }

        OutlinedTextField(guestName, { guestName = it }, label = { Text("Guest name") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        OutlinedTextField(guestEmail, { guestEmail = it }, label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        uiState.successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = {
                viewModel.clearMessages()
                if (checkIn != null && checkOut != null) {
                    viewModel.submitBooking(selectedRoomId, guestName, guestEmail, checkIn!!, checkOut!!)
                }
            },
            enabled = !uiState.isSubmitting && checkIn != null && checkOut != null && guestName.isNotBlank() && guestEmail.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text(if (uiState.isSubmitting) "Booking..." else "Confirm booking")
        }
    }
}
