package com.guesthouse.booking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.repository.ChainOccupancyTotals
import com.guesthouse.booking.data.repository.PropertyOccupancyStats
import com.guesthouse.booking.ui.theme.GlassCard

@Composable
fun ChainOccupancyBanner(totals: ChainOccupancyTotals, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "All properties — tonight",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OccupancyInlineRow(
                totals.occupiedTonight,
                totals.arrivalsToday,
                totals.departuresToday,
                totals.blockedRooms,
                totals.vacant,
                totals.totalRooms
            )
        }
    }
}

@Composable
fun PropertyOccupancyInline(stats: PropertyOccupancyStats, modifier: Modifier = Modifier) {
    OccupancyInlineRow(
        stats.occupiedTonight,
        stats.arrivalsToday,
        stats.departuresToday,
        stats.blockedRooms,
        stats.vacant,
        stats.totalRooms,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OccupancyInlineRow(
    occupied: Int,
    arrivals: Int,
    departures: Int,
    blocked: Int,
    vacant: Int,
    totalRooms: Int,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatChip("Occupied", "$occupied/$totalRooms")
        StatChip("Arrivals", arrivals.toString())
        StatChip("Departures", departures.toString())
        if (blocked > 0) StatChip("Blocked", blocked.toString())
        StatChip("Vacant", vacant.toString())
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Text(
        "$label $value",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
