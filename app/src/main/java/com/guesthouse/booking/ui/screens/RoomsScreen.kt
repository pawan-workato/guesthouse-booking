package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.RoomTypeSummary
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.data.local.entities.RoomType
import com.guesthouse.booking.viewmodel.RoomsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyRoomsScreen(
    propertyId: Long,
    viewModel: RoomsViewModel,
    onBack: () -> Unit,
    onRoomClick: (Long) -> Unit
) {
    val property by viewModel.property(propertyId).collectAsState()
    val rooms by viewModel.roomsForProperty(propertyId).collectAsState()
    val typeBreakdown = remember(rooms) { RoomTypeSummary.formatBreakdown(rooms) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(property?.name ?: "Rooms") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            property?.let {
                Text(it.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${rooms.size} rooms · ${it.region}", modifier = Modifier.padding(bottom = 4.dp))
                if (typeBreakdown.isNotBlank()) {
                    Text(typeBreakdown, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rooms, key = { it.id }) { room ->
                    RoomCard(room) { onRoomClick(room.id) }
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: RoomEntity, onClick: () -> Unit) {
    val roomType = RoomType.fromStored(room.roomType)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(room.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                AssistChip(onClick = {}, label = { Text(roomType.displayLabel()) }, enabled = false)
            }
            Text(room.description, Modifier.padding(vertical = 8.dp))
            Text(
                "$${String.format(Locale.US, "%.0f", room.pricePerNight)}/night · ${room.capacity} guests",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
