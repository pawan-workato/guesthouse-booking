package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.PropertyEntity
import com.guesthouse.booking.viewmodel.BookingWithDetails
import com.guesthouse.booking.viewmodel.TodayViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    LaunchedEffect(state.actionMessage, state.actionError) {
        if (state.actionMessage != null || state.actionError != null) { kotlinx.coroutines.delay(3000); viewModel.dismissActionFeedback() }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(LocalDate.parse(state.todayLabel).format(formatter), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        if (state.accessibleProperties.size > 1) PropertyFilterDropdown(state.accessibleProperties, state.selectedPropertyId, viewModel::selectProperty)
        state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp)) }
        state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { TodaySection("Arrivals", "No arrivals today.", state.arrivals, formatter, "Check in", viewModel::checkIn) }
            item { TodaySection("Departures", "No departures today.", state.departures, formatter, "Check out", viewModel::checkOut) }
            item { TodaySection("In-house", "No guests in-house.", state.inHouse, formatter, null, null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PropertyFilterDropdown(properties: List<PropertyEntity>, selectedPropertyId: Long?, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = properties.firstOrNull { it.id == selectedPropertyId }?.name ?: "All properties"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        OutlinedTextField(value = label, onValueChange = {}, readOnly = true, label = { Text("Property") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All properties") }, onClick = { onSelect(null); expanded = false })
            properties.forEach { property -> DropdownMenuItem(text = { Text(property.name) }, onClick = { onSelect(property.id); expanded = false }) }
        }
    }
}

@Composable private fun TodaySection(title: String, emptyMessage: String, bookings: List<BookingWithDetails>, formatter: DateTimeFormatter, actionLabel: String?, onAction: ((Long) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (bookings.isEmpty()) Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else bookings.forEach { item ->
            val booking = item.booking
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(item.roomName, fontWeight = FontWeight.SemiBold)
                    Text(booking.guestName)
                    if (booking.guestPhone.isNotBlank()) Text(booking.guestPhone)
                    Text("${LocalDate.ofEpochDay(booking.checkInEpochDay).format(formatter)} → ${LocalDate.ofEpochDay(booking.checkOutEpochDay).format(formatter)}")
                    if (actionLabel != null && onAction != null) TextButton(onClick = { onAction(booking.id) }) { Text(actionLabel) }
                }
            }
        }
    }
}
