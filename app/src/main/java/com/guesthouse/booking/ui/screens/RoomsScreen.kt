package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.RoomEntity
import com.guesthouse.booking.viewmodel.RoomsViewModel
import java.util.Locale

@Composable
fun RoomsScreen(viewModel: RoomsViewModel, onRoomClick: (Long) -> Unit) {
    val rooms by viewModel.rooms.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Our Rooms", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tap a room to see availability", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rooms, key = { it.id }) { room -> RoomCard(room) { onRoomClick(room.id) } }
        }
    }
}

@Composable
private fun RoomCard(room: RoomEntity, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(room.description, Modifier.padding(vertical = 8.dp))
            Text("$${String.format(Locale.US, "%.0f", room.pricePerNight)}/night · ${room.capacity} guests",
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
