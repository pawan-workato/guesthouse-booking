package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.viewmodel.StaffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFormScreen(
    staffId: Long?,
    viewModel: StaffViewModel,
    isFirebaseConfigured: Boolean = false,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val formState by viewModel.formUiState.collectAsState()
    val editStaff by viewModel.editStaff.collectAsState()
    val activeProperties by viewModel.activeProperties.collectAsState()
    val isEdit = staffId != null

    LaunchedEffect(staffId) {
        viewModel.clearFormState()
        if (staffId != null) viewModel.loadStaffForEdit(staffId)
        else viewModel.clearEditStaff()
    }

    LaunchedEffect(formState.savedStaffId) {
        if (formState.savedStaffId != null) {
            viewModel.clearFormState()
            onSaved()
        }
    }

    var displayName by remember(staffId, editStaff) { mutableStateOf(editStaff?.staff?.displayName ?: "") }
    var email by remember(staffId, editStaff) { mutableStateOf(editStaff?.staff?.email ?: "") }
    var password by remember(staffId) { mutableStateOf("") }
    var selectedPropertyIds by remember(staffId, editStaff) {
        mutableStateOf(editStaff?.assignedPropertyIds ?: emptySet())
    }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editStaff) {
        editStaff?.let {
            displayName = it.staff.displayName
            email = it.staff.email
            selectedPropertyIds = it.assignedPropertyIds
        }
    }

    if (isEdit && editStaff == null && !formState.isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isManager = editStaff?.staff?.role == StaffRole.PROPERTY_MANAGER.name || !isEdit
    val isChainAdmin = editStaff?.staff?.role == StaffRole.CHAIN_ADMIN.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit staff" else "Add manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (!isEdit && isFirebaseConfigured) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Firebase sign-in requires an Auth account. Adding a manager here saves locally only. " +
                            "Run scripts/seed-firebase-demo.mjs (see scripts/README.md) or use a Cloud Function to create Auth users.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
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
            if (!isEdit) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Temporary password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("At least 6 characters") }
                )
            }

            if (isChainAdmin) {
                Text(
                    "Chain admins can access all properties. Property assignment is not applicable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isManager) {
                Text("Assigned properties", style = MaterialTheme.typography.titleSmall)
                if (activeProperties.isEmpty()) {
                    Text(
                        "No active properties available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeProperties.forEach { property ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = property.id in selectedPropertyIds,
                                onCheckedChange = { checked ->
                                    selectedPropertyIds = if (checked) {
                                        selectedPropertyIds + property.id
                                    } else {
                                        selectedPropertyIds - property.id
                                    }
                                }
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text(property.name)
                                Text(
                                    property.region,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            formState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (isEdit && staffId != null) {
                        viewModel.updateStaff(staffId, email, displayName, selectedPropertyIds)
                    } else {
                        viewModel.createManager(email, displayName, password, selectedPropertyIds)
                    }
                },
                enabled = !formState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (formState.isSaving) "Saving..." else if (isEdit) "Save changes" else "Add manager")
            }

            if (isEdit && editStaff?.staff?.isActive == true) {
                val canRemove = editStaff?.staff?.role != StaffRole.CHAIN_ADMIN.name ||
                    (editStaff?.staff?.role == StaffRole.CHAIN_ADMIN.name)
                if (canRemove) {
                    OutlinedButton(
                        onClick = { showDeactivateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isChainAdmin) "Remove chain admin" else "Remove manager")
                    }
                }
            } else if (isEdit && editStaff?.staff?.isActive == false) {
                OutlinedButton(
                    onClick = {
                        viewModel.setStaffActive(staffId!!, true)
                        onSaved()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reactivate staff")
                }
            }
        }
    }

    if (showDeactivateDialog && staffId != null) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Remove staff member?") },
            text = {
                Text(
                    "This deactivates the account. They can no longer sign in. " +
                        "Existing bookings are unchanged."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setStaffActive(staffId, false)
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
