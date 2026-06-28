package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.StaffRole
import com.guesthouse.booking.data.repository.StaffWithAssignments
import com.guesthouse.booking.viewmodel.StaffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    viewModel: StaffViewModel,
    onAddStaff: () -> Unit,
    onEditStaff: (Long) -> Unit
) {
    val staff by viewModel.staffList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showInactive by viewModel.showInactive.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStaff) {
                Icon(Icons.Default.Add, contentDescription = "Add manager")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Staff", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${staff.size} ${if (showInactive) "total" else "active"} staff accounts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show removed", modifier = Modifier.weight(1f))
                Switch(checked = showInactive, onCheckedChange = viewModel::setShowInactive)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name or email") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            if (staff.isEmpty()) {
                Text(
                    "No staff yet. Tap + to add a property manager.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(staff, key = { it.staff.id }) { item ->
                        StaffCard(
                            item = item,
                            roleLabel = viewModel.roleLabel(item.staff.role),
                            onEdit = { onEditStaff(item.staff.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffCard(item: StaffWithAssignments, roleLabel: String, onEdit: () -> Unit) {
    val isManager = item.staff.role == StaffRole.PROPERTY_MANAGER.name
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.staff.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!item.staff.isActive) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("Removed") })
                    }
                }
                Text(roleLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(item.staff.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isManager) {
                    Text(
                        "${item.assignedPropertyIds.size} assigned ${if (item.assignedPropertyIds.size == 1) "property" else "properties"}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        "All properties",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit staff")
            }
        }
    }
}
