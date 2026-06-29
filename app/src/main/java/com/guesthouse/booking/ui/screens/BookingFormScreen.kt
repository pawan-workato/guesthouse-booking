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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(viewModel: BookingViewModel) {
    val properties by viewModel.properties.collectAsState()
    val selectedPropertyId by viewModel.selectedPropertyId.collectAsState()
    val rooms by viewModel.roomsForSelectedProperty.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val activeGuests by viewModel.activeGuests.collectAsState()

    var selectedRoomId by remember { mutableLongStateOf(0L) }
    var selectedGuestId by remember { mutableStateOf<Long?>(null) }
    var guestName by remember { mutableStateOf("") }
    var guestEmail by remember { mutableStateOf("") }
    var guestPhone by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf<Long?>(null) }
    var checkOut by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(properties, selectedPropertyId) {
        if (selectedPropertyId == null && properties.isNotEmpty()) {
            viewModel.selectProperty(properties.first().id)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.consumePreselectedRoom()?.let { selectedRoomId = it }
    }

    LaunchedEffect(selectedPropertyId, rooms) {
        if (selectedRoomId == 0L || rooms.none { it.id == selectedRoomId }) {
            selectedRoomId = rooms.firstOrNull()?.id ?: 0L
            checkIn = null
            checkOut = null
        }
    }

    val bookings by viewModel.observeRoomBookings(selectedRoomId).collectAsState()
    val blocks by viewModel.observeRoomBlocks(selectedRoomId).collectAsState()
    val bookedDays = bookedDaysFromRanges(bookings.map { it.checkInEpochDay to it.checkOutEpochDay })
    val blockedDays = bookedDaysFromRanges(blocks.map { it.startEpochDay to it.endEpochDay })
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("New Booking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Staff booking — enter guest details on their behalf",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (!isOnline) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    "Offline — bookings will sync when you're back online",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (properties.isNotEmpty()) {
            var propertyExpanded by remember { mutableStateOf(false) }
            val selectedProperty = properties.find { it.id == selectedPropertyId }
            ExposedDropdownMenuBox(expanded = propertyExpanded, onExpandedChange = { propertyExpanded = it }) {
                OutlinedTextField(
                    value = selectedProperty?.name ?: "Select property",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Property") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(propertyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = propertyExpanded, onDismissRequest = { propertyExpanded = false }) {
                    properties.forEach { property ->
                        DropdownMenuItem(
                            text = { Text("${property.name} (${property.region})") },
                            onClick = {
                                viewModel.selectProperty(property.id)
                                selectedRoomId = 0L
                                checkIn = null
                                checkOut = null
                                propertyExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedPropertyId != null && rooms.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            var roomExpanded by remember { mutableStateOf(false) }
            val selectedRoom = rooms.find { it.id == selectedRoomId }
            ExposedDropdownMenuBox(expanded = roomExpanded, onExpandedChange = { roomExpanded = it }) {
                OutlinedTextField(
                    value = selectedRoom?.name ?: "Select room",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Room") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roomExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                    rooms.forEach { room ->
                        DropdownMenuItem(
                            text = { Text("${room.name} — $${room.pricePerNight.toInt()}/night") },
                            onClick = {
                                selectedRoomId = room.id
                                checkIn = null
                                checkOut = null
                                roomExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedRoomId != 0L) {
            AvailabilityCalendar(
                bookedEpochDays = bookedDays,
                blockedEpochDays = blockedDays,
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
        }

        if (checkIn != null) Text("Check-in: ${LocalDate.ofEpochDay(checkIn!!).format(formatter)}")
        if (checkOut != null) Text("Check-out: ${LocalDate.ofEpochDay(checkOut!!).format(formatter)}")

        Spacer(Modifier.height(8.dp))
        var guestExpanded by remember { mutableStateOf(false) }
        val guestLabel = when (selectedGuestId) {
            null -> "Enter guest manually"
            else -> activeGuests.find { it.id == selectedGuestId }?.name ?: "Select guest"
        }
        ExposedDropdownMenuBox(expanded = guestExpanded, onExpandedChange = { guestExpanded = it }) {
            OutlinedTextField(
                value = guestLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Saved guest (optional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(guestExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = guestExpanded, onDismissRequest = { guestExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Enter guest manually") },
                    onClick = {
                        selectedGuestId = null
                        guestExpanded = false
                    }
                )
                activeGuests.forEach { guest ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(guest.name)
                                if (guest.phone.isNotBlank() || guest.email.isNotBlank()) {
                                    Text(
                                        listOf(guest.phone, guest.email).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        onClick = {
                            selectedGuestId = guest.id
                            guestName = guest.name
                            guestEmail = guest.email
                            guestPhone = guest.phone
                            guestExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(guestName, { guestName = it; selectedGuestId = null }, label = { Text("Guest name *") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        OutlinedTextField(guestPhone, { guestPhone = it; selectedGuestId = null }, label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(guestEmail, { guestEmail = it; selectedGuestId = null }, label = { Text("Email") },
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
                if (checkIn != null && checkOut != null && selectedRoomId != 0L) {
                    viewModel.submitBooking(
                        selectedRoomId, selectedGuestId, guestName, guestEmail, guestPhone, checkIn!!, checkOut!!
                    )
                }
            },
            enabled = !uiState.isSubmitting && selectedRoomId != 0L &&
                checkIn != null && checkOut != null && guestName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text(if (uiState.isSubmitting) "Saving..." else "Confirm booking")
        }
    }
}
