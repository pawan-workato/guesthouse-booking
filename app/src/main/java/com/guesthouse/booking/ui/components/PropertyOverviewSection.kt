package com.guesthouse.booking.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.repository.ChainOccupancyTotals
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.ui.theme.GlassCard

@Composable
fun PropertyOverviewSection(
    stats: List<PropertyOccupancyStats>,
    chainTotals: ChainOccupancyTotals?,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Overview — tonight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        chainTotals?.let { totals ->
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Chain totals", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OccupancyStatRow("Occupied", totals.occupiedTonight, totals.totalRooms)
                    OccupancyStatRow("Arrivals today", totals.arrivalsToday)
                    OccupancyStatRow("Departures today", totals.departuresToday)
                    OccupancyStatRow("Blocked", totals.blockedRooms)
                    OccupancyStatRow("Vacant", totals.vacant, totals.totalRooms)
                }
            }
        }
        stats.forEach { propertyStats ->
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(propertyStats.propertyName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OccupancyStatRow("Occupied", propertyStats.occupiedTonight, propertyStats.totalRooms)
                    OccupancyStatRow("Arrivals today", propertyStats.arrivalsToday)
                    OccupancyStatRow("Departures today", propertyStats.departuresToday)
                    OccupancyStatRow("Blocked", propertyStats.blockedRooms)
                    OccupancyStatRow("Vacant", propertyStats.vacant, propertyStats.totalRooms)
                }
            }
        }
    }
}

@Composable
private fun OccupancyStatRow(label: String, value: Int, total: Int? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (total != null) "$value / $total" else value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
