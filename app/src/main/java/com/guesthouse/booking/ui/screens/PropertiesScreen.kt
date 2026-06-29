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
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.ui.components.ChainOccupancyBanner
import com.guesthouse.booking.ui.components.PropertyOccupancyInline
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.viewmodel.PropertiesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesScreen(
    viewModel: PropertiesViewModel,
    isChainAdmin: Boolean,
    onPropertyClick: (Long) -> Unit,
    onAddProperty: () -> Unit,
    onEditProperty: (Long) -> Unit,
    onOpenReports: (() -> Unit)? = null
) {
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showInactive by viewModel.showInactive.collectAsStateWithLifecycle()
    val occupancyStats by viewModel.occupancyStats.collectAsStateWithLifecycle()
    val chainTotals by viewModel.chainTotals.collectAsStateWithLifecycle()
    val statsByPropertyId = remember(occupancyStats) { occupancyStats.associateBy { it.propertyId } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header_title") {
                Text(
                    "Your properties",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item(key = "header_subtitle") {
                Text(
                    if (isChainAdmin) "Chain admin — ${properties.size} ${if (showInactive) "total" else "active"} sites"
                    else "${properties.size} assigned ${if (properties.size == 1) "site" else "sites"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (onOpenReports != null) {
                item(key = "open_reports") {
                    TextButton(onClick = onOpenReports, modifier = Modifier.fillMaxWidth()) {
                        Text("Occupancy report")
                    }
                }
            }
            if (isChainAdmin) {
                item(key = "show_inactive") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show removed", modifier = Modifier.weight(1f))
                        Switch(checked = showInactive, onCheckedChange = viewModel::setShowInactive)
                    }
                }
                item(key = "add_property") {
                    OutlinedButton(
                        onClick = onAddProperty,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add property")
                    }
                }
            }
            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name, region, or city") },
                    singleLine = true
                )
            }
            if (isChainAdmin && chainTotals != null && properties.size > 1) {
                item(key = "chain_totals") {
                    ChainOccupancyBanner(totals = chainTotals!!)
                }
            }
            if (properties.isEmpty()) {
                item(key = "empty") {
                    Text("No properties available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(properties, key = { it.id }) { property ->
                    PropertyCard(
                        property = property,
                        stats = if (property.isActive) statsByPropertyId[property.id] else null,
                        isChainAdmin = isChainAdmin,
                        onClick = { if (property.isActive) onPropertyClick(property.id) },
                        onEdit = { onEditProperty(property.id) }
                    )
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
    stats: PropertyOccupancyStats?,
    isChainAdmin: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = property.isActive, onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            property.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!property.isActive) {
                            AssistChip(onClick = {}, enabled = false, label = { Text("Removed") })
                        }
                    }
                    Text(
                        property.region,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        property.address,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        "Check-in ${property.checkInTime} · Check-out ${property.checkOutTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (isChainAdmin) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit property")
                    }
                } else if (property.isActive) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open rooms")
                }
            }
            if (stats != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                PropertyOccupancyInline(stats = stats)
            }
        }
    }
}
