package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guesthouse.booking.data.local.entities.BookingStatus
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.data.repository.GuestStayBooking
import com.guesthouse.booking.ui.components.SimilarGuestWarning
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.GuestsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestFormScreen(
    guestId: Long?,
    viewModel: GuestsViewModel,
    readOnly: Boolean = false,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onBookAgain: (() -> Unit)? = null
) {
    val formState by viewModel.formUiState.collectAsStateWithLifecycle()
    val editGuest by viewModel.editGuest.collectAsStateWithLifecycle()
    val stayHistory by viewModel.guestStayHistory.collectAsStateWithLifecycle()
    val isChainAdmin by viewModel.isChainAdmin.collectAsStateWithLifecycle()
    val similarGuests by viewModel.similarGuests.collectAsStateWithLifecycle()
    val isEdit = guestId != null
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    LaunchedEffect(guestId) {
        viewModel.clearFormState()
        if (guestId != null) viewModel.loadGuestForEdit(guestId)
        else { viewModel.clearEditGuest(); viewModel.clearGuestLookup() }
    }

    LaunchedEffect(formState.savedGuestId) {
        if (formState.savedGuestId != null) {
            viewModel.clearFormState()
            onSaved()
        }
    }

    var name by remember(guestId, editGuest) { mutableStateOf(editGuest?.name ?: "") }
    var email by remember(guestId, editGuest) { mutableStateOf(editGuest?.email ?: "") }
    var phone by remember(guestId, editGuest) { mutableStateOf(editGuest?.phone ?: "") }
    var notes by remember(guestId, editGuest) { mutableStateOf(editGuest?.notes ?: "") }
    var preferences by remember(guestId, editGuest) { mutableStateOf(editGuest?.preferences ?: "") }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(name, email, phone) {
        if (!isEdit && !readOnly) viewModel.updateGuestLookup(name, email, phone)
    }

    LaunchedEffect(editGuest) {
        editGuest?.let {
            name = it.name
            email = it.email
            phone = it.phone
            notes = it.notes
            preferences = it.preferences
        }
    }

    if (isEdit && editGuest == null && !formState.isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text(when {
                    readOnly -> "Guest details"
                    isEdit -> "Edit guest"
                    else -> "Add guest"
                }) },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Guest name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = readOnly,
                enabled = !readOnly
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = readOnly,
                enabled = !readOnly
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = readOnly,
                enabled = !readOnly
            )
            if (!isEdit && !readOnly) {
                SimilarGuestWarning(
                    similarGuests = similarGuests,
                    onUseExisting = { guest ->
                        name = guest.name
                        email = guest.email
                        phone = guest.phone
                        viewModel.clearGuestLookup()
                    }
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("Preferences, accessibility needs, etc.") },
                readOnly = readOnly,
                enabled = !readOnly
            )

            if (isEdit && onBookAgain != null) {
                OutlinedButton(onClick = onBookAgain, modifier = Modifier.fillMaxWidth()) {
                    Text("Book again")
                }
            }

            if (isEdit) {
                GuestStayHistorySection(
                    stays = stayHistory,
                    isChainAdmin = isChainAdmin,
                    formatter = dateFormatter
                )
            }

            formState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (!readOnly) {
                Button(
                    onClick = {
                        if (isEdit && editGuest != null) {
                            viewModel.updateGuest(
                                editGuest!!.copy(name = name, email = email, phone = phone, notes = notes, preferences = preferences)
                            )
                        } else {
                            viewModel.createGuest(name, email, phone, notes, preferences)
                        }
                    },
                    enabled = !formState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (formState.isSaving) "Saving..." else if (isEdit) "Save changes" else "Add guest")
                }

                if (isEdit && editGuest?.isActive == true && isChainAdmin) {
                OutlinedButton(
                    onClick = { showDeactivateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove guest")
                }
                } else if (isEdit && editGuest?.isActive == false && isChainAdmin) {
                OutlinedButton(
                    onClick = {
                        viewModel.setGuestActive(guestId!!, true)
                        onSaved()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reactivate guest")
                }
                }
            }
        }
    }

    if (showDeactivateDialog && guestId != null) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Remove guest?") },
            text = {
                Text(
                    "This hides the guest from the active list and booking picker. " +
                        "Existing bookings keep their guest details."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setGuestActive(guestId, false)
                    showDeactivateDialog = false
                    onSaved()
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GuestStayHistorySection(
    stays: List<GuestStayBooking>,
    isChainAdmin: Boolean,
    formatter: DateTimeFormatter
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Stay history",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            if (isChainAdmin) "All properties" else "Your assigned properties only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (stays.isEmpty()) {
            Text(
                if (isChainAdmin) "No recorded stays for this guest."
                else "No stays at your assigned properties.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            stays.forEach { stay ->
                val booking = stay.booking
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stay.propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(stay.roomName, fontWeight = FontWeight.Medium)
                        Text(
                            "${LocalDate.ofEpochDay(booking.checkInEpochDay).format(formatter)} → " +
                                LocalDate.ofEpochDay(booking.checkOutEpochDay).format(formatter)
                        )
                        if (booking.bookingReference.isNotBlank()) {
                            Text("Ref: ${booking.bookingReference}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            booking.status.replace('_', ' '),
                            style = MaterialTheme.typography.bodySmall,
                            color = when (booking.status) {
                                BookingStatus.CANCELLED.name -> MaterialTheme.colorScheme.error
                                BookingStatus.CHECKED_IN.name -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
