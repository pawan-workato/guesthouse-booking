package com.guesthouse.booking.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.data.local.entities.GuestEntity
import com.guesthouse.booking.ui.theme.GlassCard

@Composable
fun SimilarGuestWarning(
    similarGuests: List<GuestEntity>,
    onUseExisting: (GuestEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (similarGuests.isEmpty()) return
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Similar guest found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            similarGuests.forEach { guest ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(guest.name, style = MaterialTheme.typography.bodyMedium)
                        val detail = listOf(guest.email, guest.phone).filter { it.isNotBlank() }.joinToString(" · ")
                        if (detail.isNotBlank()) {
                            Text(detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    TextButton(onClick = { onUseExisting(guest) }) {
                        Text("Use existing")
                    }
                }
            }
        }
    }
}
