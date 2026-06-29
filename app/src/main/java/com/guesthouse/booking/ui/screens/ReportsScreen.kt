package com.guesthouse.booking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.ui.components.ChainOccupancyBanner
import com.guesthouse.booking.ui.components.PropertyOccupancyInline
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.ReportsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit
) {
    val reportDate by viewModel.reportDate.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val occupancyStats by viewModel.occupancyStats.collectAsStateWithLifecycle()
    val chainTotals by viewModel.chainTotals.collectAsStateWithLifecycle()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val isToday = remember(reportDate) { reportDate == java.time.LocalDate.now() }

    GlassScaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("Occupancy report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (isToday) "Tonight & today" else "Selected date",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { viewModel.shiftReportDate(-1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                            }
                            Text(
                                reportDate.format(dateFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { viewModel.shiftReportDate(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                            }
                        }
                        Text(
                            "Occupied, arrivals, departures, blocks, and vacant rooms for each active property.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (chainTotals != null && properties.size > 1) {
                item(key = "chain_totals") {
                    ChainOccupancyBanner(totals = chainTotals!!)
                }
            }
            if (properties.isEmpty()) {
                item {
                    Text(
                        "No active properties to report on.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(occupancyStats, key = { it.propertyId }) { stats ->
                    ReportPropertyCard(stats)
                }
            }
        }
    }
}

@Composable
private fun ReportPropertyCard(stats: PropertyOccupancyStats) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stats.propertyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            PropertyOccupancyInline(stats)
        }
    }
}
