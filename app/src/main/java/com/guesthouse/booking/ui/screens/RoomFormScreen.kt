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
import com.guesthouse.booking.data.local.entities.RoomType
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.RoomsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomFormScreen(
    propertyId: Long,
    roomId: Long?,
    viewModel: RoomsViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val formState by viewModel.formUiState.collectAsState()
    val editRoom by viewModel.editRoom.collectAsState()
    val isEdit = roomId != null

    LaunchedEffect(roomId) {
        viewModel.clearFormState()
        if (roomId != null) viewModel.loadRoomForEdit(roomId)
        else viewModel.clearEditRoom()
    }

    LaunchedEffect(formState.savedRoomId) {
        if (formState.savedRoomId != null) {
            viewModel.clearFormState()
            onSaved()
        }
    }

    var name by remember(roomId, editRoom) { mutableStateOf(editRoom?.name ?: "") }
    var description by remember(roomId, editRoom) { mutableStateOf(editRoom?.description ?: "") }
    var priceText by remember(roomId, editRoom) {
        mutableStateOf(editRoom?.pricePerNight?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var capacityText by remember(roomId, editRoom) { mutableStateOf(editRoom?.capacity?.toString() ?: "2") }
    var roomType by remember(roomId, editRoom) {
        mutableStateOf(RoomType.fromStored(editRoom?.roomType ?: RoomType.DOUBLE.name))
    }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(editRoom) {
        editRoom?.let {
            name = it.name
            description = it.description
            priceText = if (it.pricePerNight % 1.0 == 0.0) {
                it.pricePerNight.toInt().toString()
            } else {
                it.pricePerNight.toString()
            }
            capacityText = it.capacity.toString()
            roomType = RoomType.fromStored(it.roomType)
        }
    }

    if (isEdit && editRoom == null && !formState.isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text(if (isEdit) "Edit room" else "Add room") },
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
                label = { Text("Room name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Mountain View Double") }
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Price per night ($)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = capacityText,
                onValueChange = { capacityText = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("Max guests") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = roomType.displayLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Room type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    RoomType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayLabel()) },
                            onClick = {
                                roomType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }
            formState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull()
                    val capacity = capacityText.toIntOrNull()
                    when {
                        price == null -> return@Button
                        capacity == null || capacity < 1 -> return@Button
                        isEdit && editRoom != null -> viewModel.updateRoom(
                            editRoom!!.copy(
                                name = name,
                                description = description,
                                pricePerNight = price,
                                capacity = capacity,
                                roomType = roomType.name
                            )
                        )
                        else -> viewModel.createRoom(
                            propertyId = propertyId,
                            name = name,
                            description = description,
                            pricePerNight = price,
                            capacity = capacity,
                            roomType = roomType.name
                        )
                    }
                },
                enabled = !formState.isSaving && name.isNotBlank() && priceText.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEdit) "Save changes" else "Add room")
                }
            }
        }
    }
}
