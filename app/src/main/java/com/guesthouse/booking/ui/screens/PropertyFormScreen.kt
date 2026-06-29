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
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.PropertiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyFormScreen(
    propertyId: Long?,
    viewModel: PropertiesViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val formState by viewModel.formUiState.collectAsState()
    val editProperty by viewModel.editProperty.collectAsState()
    val isEdit = propertyId != null

    LaunchedEffect(propertyId) {
        viewModel.clearFormState()
        if (propertyId != null) viewModel.loadPropertyForEdit(propertyId)
        else viewModel.clearEditProperty()
    }

    LaunchedEffect(formState.savedPropertyId) {
        if (formState.savedPropertyId != null) {
            viewModel.clearFormState()
            onSaved()
        }
    }

    var name by remember(propertyId, editProperty) { mutableStateOf(editProperty?.name ?: "") }
    var address by remember(propertyId, editProperty) { mutableStateOf(editProperty?.address ?: "") }
    var region by remember(propertyId, editProperty) { mutableStateOf(editProperty?.region ?: "") }
    var checkInTime by remember(propertyId, editProperty) { mutableStateOf(editProperty?.checkInTime ?: "15:00") }
    var checkOutTime by remember(propertyId, editProperty) { mutableStateOf(editProperty?.checkOutTime ?: "11:00") }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editProperty) {
        editProperty?.let {
            name = it.name
            address = it.address
            region = it.region
            checkInTime = it.checkInTime
            checkOutTime = it.checkOutTime
        }
    }

    if (isEdit && editProperty == null && !formState.isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text(if (isEdit) "Edit property" else "Add property") },
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
                label = { Text("Property name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = region,
                onValueChange = { region = it },
                label = { Text("Region") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Mountain West") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = checkInTime,
                    onValueChange = { checkInTime = it },
                    label = { Text("Check-in") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("15:00") }
                )
                OutlinedTextField(
                    value = checkOutTime,
                    onValueChange = { checkOutTime = it },
                    label = { Text("Check-out") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("11:00") }
                )
            }

            formState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (isEdit && editProperty != null) {
                        viewModel.updateProperty(
                            editProperty!!.copy(
                                name = name,
                                address = address,
                                region = region,
                                checkInTime = checkInTime,
                                checkOutTime = checkOutTime
                            )
                        )
                    } else {
                        viewModel.createProperty(name, address, region, checkInTime, checkOutTime)
                    }
                },
                enabled = !formState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (formState.isSaving) "Saving..." else if (isEdit) "Save changes" else "Add property")
            }

            if (isEdit && editProperty?.isActive == true) {
                OutlinedButton(
                    onClick = { showDeactivateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove property")
                }
            } else if (isEdit && editProperty?.isActive == false) {
                OutlinedButton(
                    onClick = {
                        viewModel.setPropertyActive(propertyId!!, true)
                        onSaved()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reactivate property")
                }
            }
        }
    }

    if (showDeactivateDialog && propertyId != null) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Remove property?") },
            text = {
                Text(
                    "This hides the property from staff and stops new bookings. " +
                        "Existing bookings and rooms are kept."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setPropertyActive(propertyId, false)
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
