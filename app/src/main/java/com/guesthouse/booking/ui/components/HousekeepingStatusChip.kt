package com.guesthouse.booking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.HousekeepingStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HousekeepingStatusSelector(
    current: String,
    onSelect: (HousekeepingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = runCatching { HousekeepingStatus.valueOf(current) }.getOrDefault(HousekeepingStatus.CLEAN)
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HousekeepingStatus.entries.forEach { status ->
            FilterChip(
                selected = status == selected,
                onClick = { onSelect(status) },
                label = { Text(status.label, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}
