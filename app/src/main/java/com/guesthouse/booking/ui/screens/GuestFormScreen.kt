package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.viewmodel.GuestsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestFormScreen(
    guestId: Long?,
    viewModel: GuestsViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val formState by viewModel.formUiState.collectAsState()
    val editGuest by viewModel.editGuest.collectAsState()
    val isEdit = guestId != null

    LaunchedEffect(guestId) {
        viewModel.clearFormState()
        if (guestId != null) viewModel.loadGuestForEdit(guestId)
        else viewModel.clearEditGuest()
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
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editGuest) {
        editGuest?.let {
            name = it.name
            email = it.email
            phone = it.phone
            notes = it.notes
        }
    }

    if (isEdit && editGuest == null && !formState.isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit guest" else "Add guest") },
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
                singleLine = true
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("Preferences, accessibility needs, etc.") }
            )

            formState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (isEdit && editGuest != null) {
                        viewModel.updateGuest(
                            editGuest!!.copy(name = name, email = email, phone = phone, notes = notes)
                        )
                    } else {
                        viewModel.createGuest(name, email, phone, notes)
                    }
                },
                enabled = !formState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (formState.isSaving) "Saving..." else if (isEdit) "Save changes" else "Add guest")
            }

            if (isEdit && editGuest?.isActive == true) {
                OutlinedButton(
                    onClick = { showDeactivateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove guest")
                }
            } else if (isEdit && editGuest?.isActive == false) {
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
