package com.guesthouse.booking.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.repository.DayRevenueForecast
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.data.repository.PropertyRevenueStats
import com.guesthouse.booking.ui.components.ChainOccupancyBanner
import com.guesthouse.booking.ui.components.PropertyOccupancyInline
import com.guesthouse.booking.ui.theme.GlassCard
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.viewmodel.ReportsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBack: () -> Unit,
    onOpenAuditLog: (() -> Unit)? = null
) {
    val reportDate by viewModel.reportDate.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val occupancyStats by viewModel.occupancyStats.collectAsStateWithLifecycle()
    val revenueStats by viewModel.revenueStats.collectAsStateWithLifecycle()
    val chainTotals by viewModel.chainTotals.collectAsStateWithLifecycle()
    val chainRevenueTotal by viewModel.chainRevenueTotal.collectAsStateWithLifecycle()
    val showWeekAhead by viewModel.showWeekAheadForecast.collectAsStateWithLifecycle()
    val weekAhead by viewModel.weekAheadForecast.collectAsStateWithLifecycle()
    val csvExport by viewModel.csvExport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val payload = csvExport ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(payload.content.toByteArray())
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share export"))
        }
        viewModel.clearCsvExport()
    }
    LaunchedEffect(csvExport) {
        csvExport?.let { createDocumentLauncher.launch(it.fileName) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("EEE MMM d") }
    val isToday = remember(reportDate) { reportDate == LocalDate.now() }

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
                            "Occupied, arrivals, departures, blocks, vacant rooms, and revenue for active stays.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Revenue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Chain total: $%.2f".format(chainRevenueTotal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        revenueStats.filter { it.bookingCount > 0 }.forEach { stats ->
                            Text(
                                "${stats.propertyName}: $%.2f (${stats.bookingCount} stays)".format(stats.totalRevenue),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (revenueStats.all { it.bookingCount == 0 }) {
                            Text(
                                "No active stays on this date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("7-day revenue forecast", modifier = Modifier.weight(1f))
                    Switch(checked = showWeekAhead, onCheckedChange = viewModel::setShowWeekAheadForecast)
                }
            }
            if (showWeekAhead && weekAhead.isNotEmpty()) {
                items(weekAhead, key = { it.epochDay }) { day ->
                    ForecastDayCard(day, shortDateFormatter)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::prepareBookingsCsvExport) {
                        Text("Export bookings")
                    }
                    OutlinedButton(onClick = viewModel::prepareGuestsCsvExport) {
                        Text("Export guests")
                    }
                }
            }
            if (onOpenAuditLog != null) {
                item {
                    TextButton(onClick = onOpenAuditLog, modifier = Modifier.fillMaxWidth()) {
                        Text("Audit log")
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
private fun ForecastDayCard(day: DayRevenueForecast, formatter: DateTimeFormatter) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                LocalDate.ofEpochDay(day.epochDay).format(formatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Forecast: $%.2f".format(day.totalRevenue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
