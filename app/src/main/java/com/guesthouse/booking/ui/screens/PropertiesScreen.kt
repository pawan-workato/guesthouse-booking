package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.viewmodel.PropertiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    viewModel: PropertiesViewModel,
    isChainAdmin: Boolean,
    onPropertyClick: (Long) -> Unit,
    onAddProperty: () -> Unit,
    onEditProperty: (Long) -> Unit
) {
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showInactive by viewModel.showInactive.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Your properties",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isChainAdmin) "Chain admin — ${properties.size} ${if (showInactive) "total" else "active"} sites"
                else "${properties.size} assigned ${if (properties.size == 1) "site" else "sites"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (isChainAdmin) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show removed", modifier = Modifier.weight(1f))
                    Switch(checked = showInactive, onCheckedChange = viewModel::setShowInactive)
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, region, or city") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            if (properties.isEmpty()) {
                Text("No properties available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(properties, key = { it.id }) { property ->
                        PropertyCard(
                            property = property,
                            isChainAdmin = isChainAdmin,
                            onClick = { if (property.isActive) onPropertyClick(property.id) },
                            onEdit = { onEditProperty(property.id) }
                        )
                    }
                }
            }
        }
        if (isChainAdmin) {
            FloatingActionButton(
                onClick = onAddProperty,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add property")
            }
        }
    }
}

@Composable
private fun PropertyCard(
    property: PropertyEntity,
    isChainAdmin: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(enabled = property.isActive, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(property.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!property.isActive) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("Removed") })
                    }
                }
                Text(property.region, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(property.address, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                Text(
                    "Check-in ${property.checkInTime} · Check-out ${property.checkOutTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (isChainAdmin) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit property")
                }
            } else if (property.isActive) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}
