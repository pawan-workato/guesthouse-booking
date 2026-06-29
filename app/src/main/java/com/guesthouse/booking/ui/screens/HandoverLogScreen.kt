package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.HandoverLogViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandoverLogScreen(viewModel: HandoverLogViewModel, onBack: () -> Unit) {
    val properties by viewModel.accessibleProperties.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPropertyId by remember { mutableStateOf<Long?>(properties.firstOrNull()?.id) }
    var noteText by remember { mutableStateOf("") }
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }

    LaunchedEffect(properties) {
        if (selectedPropertyId == null || properties.none { it.id == selectedPropertyId }) {
            selectedPropertyId = properties.firstOrNull()?.id
            viewModel.selectProperty(selectedPropertyId)
        }
    }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error ?: return@LaunchedEffect
        noteText = if (uiState.message != null) "" else noteText
        kotlinx.coroutines.delay(2500)
        viewModel.clearMessage()
    }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("Handover log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Add shift note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (properties.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            val label = properties.firstOrNull { it.id == selectedPropertyId }?.name ?: "Select property"
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                OutlinedTextField(
                                    value = label,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Property") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    properties.forEach { property ->
                                        DropdownMenuItem(
                                            text = { Text(property.name) },
                                            onClick = {
                                                selectedPropertyId = property.id
                                                viewModel.selectProperty(property.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Note for next shift") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Button(
                            onClick = {
                                selectedPropertyId?.let { viewModel.addNote(it, noteText) }
                            },
                            enabled = !uiState.isSaving && selectedPropertyId != null && noteText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save note") }
                        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            if (notes.isEmpty()) {
                item { Text("No handover notes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(notes, key = { it.id }) { entry ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.staffName, fontWeight = FontWeight.SemiBold)
                            Text(
                                Instant.ofEpochMilli(entry.createdAtEpochMs)
                                    .atZone(ZoneId.systemDefault())
                                    .format(formatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(entry.note)
                        }
                    }
                }
            }
        }
    }
}
