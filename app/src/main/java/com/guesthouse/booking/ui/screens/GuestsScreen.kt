package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.viewmodel.GuestsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestsScreen(
    viewModel: GuestsViewModel,
    onAddGuest: () -> Unit,
    onEditGuest: (Long) -> Unit
) {
    val guests by viewModel.guests.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showInactive by viewModel.showInactive.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Guests",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${guests.size} ${if (showInactive) "total" else "active"} guest profiles",
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
                placeholder = { Text("Search by name, email, or phone") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            if (guests.isEmpty()) {
                Text(
                    "No guests yet. Tap + to add a guest profile.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(guests, key = { it.id }) { guest ->
                        GuestCard(
                            guest = guest,
                            onEdit = { onEditGuest(guest.id) }
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddGuest,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add guest")
        }
    }
}

@Composable
private fun GuestCard(guest: GuestEntity, onEdit: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(guest.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!guest.isActive) {
                        AssistChip(onClick = {}, enabled = false, label = { Text("Removed") })
                    }
                }
                if (guest.phone.isNotBlank()) {
                    Text(guest.phone, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                if (guest.email.isNotBlank()) {
                    Text(guest.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (guest.notes.isNotBlank()) {
                    Text(
                        guest.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit guest")
            }
        }
    }
}
