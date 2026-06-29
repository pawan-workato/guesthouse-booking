package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.RoomTypeSummary
import com.guesthouse.booking.data.local.entities.RoomType
import com.guesthouse.booking.ui.components.AvailabilityCalendar
import com.guesthouse.booking.ui.components.SimilarGuestWarning
import com.guesthouse.booking.ui.components.bookedDaysFromRanges
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.BookingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingFormScreen(
    viewModel: BookingViewModel,
    bookingId: Long? = null,
    onSaved: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val properties by viewModel.properties.collectAsState()
    val filteredProperties by viewModel.filteredProperties.collectAsState()
    val propertySearchQuery by viewModel.propertySearchQuery.collectAsState()
    val selectedPropertyId by viewModel.selectedPropertyId.collectAsState()
    val allRooms by viewModel.roomsForSelectedProperty.collectAsState()
    val filteredRooms by viewModel.filteredRooms.collectAsState()
    val roomSearchQuery by viewModel.roomSearchQuery.collectAsState()
    val roomTypeFilter by viewModel.roomTypeFilter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val activeGuests by viewModel.activeGuests.collectAsState()
    val similarGuests by viewModel.similarGuests.collectAsState()
    val editBooking by viewModel.editBooking.collectAsState()
    val isEdit = bookingId != null

    var selectedRoomId by remember(bookingId) { mutableLongStateOf(0L) }
    var selectedGuestId by remember(bookingId) { mutableStateOf<Long?>(null) }
    var guestName by remember(bookingId) { mutableStateOf("") }
    var guestEmail by remember(bookingId) { mutableStateOf("") }
    var guestPhone by remember(bookingId) { mutableStateOf("") }
    var checkIn by remember(bookingId) { mutableStateOf<Long?>(null) }
    var checkOut by remember(bookingId) { mutableStateOf<Long?>(null) }

    LaunchedEffect(bookingId) {
        viewModel.clearMessages()
        if (bookingId != null) viewModel.loadBookingForEdit(bookingId)
        else viewModel.clearEditBooking()
    }

    LaunchedEffect(editBooking) {
        editBooking?.let { booking ->
            selectedRoomId = booking.roomId
            selectedGuestId = booking.guestId
            guestName = booking.guestName
            guestEmail = booking.guestEmail
            guestPhone = booking.guestPhone
            checkIn = booking.checkInEpochDay
            checkOut = booking.checkOutEpochDay
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (isEdit && uiState.successMessage != null) {
            viewModel.clearEditBooking()
            onSaved?.invoke()
        }
    }

    LaunchedEffect(properties, selectedPropertyId) {
        if (!isEdit && selectedPropertyId == null && properties.isNotEmpty()) {
            viewModel.selectProperty(properties.first().id)
        }
    }

    var consumedGuestPreselect by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isEdit) viewModel.consumePreselectedRoom()?.let { selectedRoomId = it }
    }

    LaunchedEffect(activeGuests, isEdit, consumedGuestPreselect) {
        if (!isEdit && !consumedGuestPreselect) {
            viewModel.consumePreselectedGuest()?.let { guestId ->
                consumedGuestPreselect = true
                selectedGuestId = guestId
                activeGuests.find { it.id == guestId }?.let { guest ->
                    guestName = guest.name
                    guestEmail = guest.email
                    guestPhone = guest.phone
                }
            }
        }
    }

    LaunchedEffect(guestName, guestEmail, guestPhone, selectedGuestId) {
        if (!isEdit) {
            viewModel.updateGuestLookup(guestName, guestEmail, guestPhone, manualEntry = selectedGuestId == null)
        }
    }

    LaunchedEffect(selectedPropertyId, filteredRooms, isEdit, editBooking) {
        if (isEdit && editBooking != null) return@LaunchedEffect
        if (selectedRoomId == 0L || filteredRooms.none { it.id == selectedRoomId }) {
            selectedRoomId = filteredRooms.firstOrNull()?.id ?: 0L
            if (!isEdit) {
                checkIn = null
                checkOut = null
            }
        }
    }

    val roomTypeBreakdown = remember(allRooms) { RoomTypeSummary.formatBreakdown(allRooms) }
    val roomTypeCounts = remember(allRooms) { RoomTypeSummary.countByType(allRooms) }

    val bookings by viewModel.observeRoomBookings(selectedRoomId).collectAsState()
    val blocks by viewModel.observeRoomBlocks(selectedRoomId).collectAsState()
    val bookedDays = bookedDaysFromRanges(
        bookings
            .filter { !isEdit || it.id != bookingId }
            .map { it.checkInEpochDay to it.checkOutEpochDay }
    )
    val blockedDays = bookedDaysFromRanges(blocks.map { it.startEpochDay to it.endEpochDay })
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    if (isEdit && editBooking == null && !uiState.isSubmitting && uiState.errorMessage == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val formContent: @Composable ColumnScope.() -> Unit = {
        if (!isOnline) {
            GlassCard(
                containerColor = MaterialTheme.colorScheme.errorContainer,
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
            OutlinedTextField(
                value = propertySearchQuery,
                onValueChange = viewModel::setPropertySearchQuery,
                label = { Text("Search properties") },
                placeholder = { Text("Name, region, or city") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
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
                    if (filteredProperties.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No properties match your search") },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredProperties.forEach { property ->
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
        }

        if (selectedPropertyId != null && allRooms.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${allRooms.size} rooms" + if (roomTypeBreakdown.isNotBlank()) " · $roomTypeBreakdown" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (roomTypeCounts.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roomTypeCounts.forEach { (type, count) ->
                        FilterChip(
                            selected = roomTypeFilter == type,
                            onClick = { viewModel.toggleRoomTypeFilter(type) },
                            label = { Text("${type.displayLabel()} ($count)") }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = roomSearchQuery,
                onValueChange = viewModel::setRoomSearchQuery,
                label = { Text("Search rooms") },
                placeholder = { Text("Name, type, or description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            var roomExpanded by remember { mutableStateOf(false) }
            val selectedRoom = allRooms.find { it.id == selectedRoomId }
            ExposedDropdownMenuBox(expanded = roomExpanded, onExpandedChange = { roomExpanded = it }) {
                OutlinedTextField(
                    value = selectedRoom?.let { room ->
                        val type = RoomType.fromStored(room.roomType)
                        "${room.name} (${type.displayLabel()})"
                    } ?: "Select room",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Room") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roomExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                    if (filteredRooms.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No rooms match your search") },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredRooms.forEach { room ->
                            val type = RoomType.fromStored(room.roomType)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${room.name} — ${type.displayLabel()}")
                                        Text(
                                            "$${room.pricePerNight.toInt()}/night · up to ${room.capacity} guests",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
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
        } else if (selectedPropertyId != null) {
            Text(
                "No rooms at this property yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
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

        if (!isEdit && selectedGuestId == null) {
            SimilarGuestWarning(
                similarGuests = similarGuests,
                onUseExisting = { guest ->
                    selectedGuestId = guest.id
                    guestName = guest.name
                    guestEmail = guest.email
                    guestPhone = guest.phone
                    viewModel.clearGuestLookup()
                }
            )
        }

        OutlinedTextField(guestName, { guestName = it; selectedGuestId = null }, label = { Text("Guest name *") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        OutlinedTextField(guestPhone, { guestPhone = it; selectedGuestId = null }, label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(guestEmail, { guestEmail = it; selectedGuestId = null }, label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        uiState.successMessage?.let {
            if (!isEdit) {
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            }
        }
        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = {
                viewModel.clearMessages()
                if (checkIn != null && checkOut != null && selectedRoomId != 0L) {
                    if (isEdit && bookingId != null) {
                        viewModel.updateBooking(
                            bookingId, selectedRoomId, selectedGuestId, guestName, guestEmail, guestPhone,
                            checkIn!!, checkOut!!
                        )
                    } else {
                        viewModel.submitBooking(
                            selectedRoomId, selectedGuestId, guestName, guestEmail, guestPhone, checkIn!!, checkOut!!
                        )
                    }
                }
            },
            enabled = !uiState.isSubmitting && selectedRoomId != 0L &&
                checkIn != null && checkOut != null && guestName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text(
                when {
                    uiState.isSubmitting -> "Saving..."
                    isEdit -> "Save changes"
                    else -> "Confirm booking"
                }
            )
        }
    }

    if (isEdit) {
        GlassScaffold(
            topBar = {
                GlassTopAppBar(
                    title = { Text("Edit booking") },
                    navigationIcon = {
                        IconButton(onClick = { onBack?.invoke() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                content = formContent
            )
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("New Booking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Staff booking — enter guest details on their behalf",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            formContent()
        }
    }
}
