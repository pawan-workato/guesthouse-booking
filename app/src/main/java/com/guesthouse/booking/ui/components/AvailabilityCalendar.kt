package com.guesthouse.booking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.ui.theme.AvailableGreen
import com.guesthouse.booking.ui.theme.BlockedOrange
import com.guesthouse.booking.ui.theme.BookedRed
import com.guesthouse.booking.ui.theme.Sage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AvailabilityCalendar(
    bookedEpochDays: Set<Long>,
    blockedEpochDays: Set<Long> = emptySet(),
    selectedCheckIn: Long?,
    selectedCheckOut: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now().toEpochDay()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val firstDay = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val startOffset = firstDay.dayOfWeek.value % 7
        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

        Column {
            for (weekStart in 0 until totalCells step 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (offset in 0 until 7) {
                        val cellIndex = weekStart + offset
                        val dayNumber = cellIndex - startOffset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val epochDay = date.toEpochDay()
                            val isBooked = bookedEpochDays.contains(epochDay)
                            val isBlocked = blockedEpochDays.contains(epochDay)
                            val isUnavailable = isBooked || isBlocked
                            val isPast = epochDay < today
                            val inRange = selectedCheckIn != null && selectedCheckOut != null &&
                                epochDay >= selectedCheckIn && epochDay < selectedCheckOut
                            val isSelected = epochDay == selectedCheckIn || epochDay == selectedCheckOut

                            val bgColor = when {
                                isBooked -> BookedRed.copy(alpha = 0.25f)
                                isBlocked -> BlockedOrange.copy(alpha = 0.25f)
                                inRange || isSelected -> Sage
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val clickable = !isUnavailable && !isPast
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .then(
                                        if (clickable) {
                                            Modifier.clickable { onDateSelected(epochDay) }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                DayCell(dayNumber, isBooked, isBlocked, isPast)
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendDot(color = AvailableGreen, label = "Available")
            LegendDot(color = BookedRed, label = "Booked")
            LegendDot(color = BlockedOrange, label = "Blocked")
        }
    }
}

@Composable
private fun DayCell(dayNumber: Int, isBooked: Boolean, isBlocked: Boolean, isPast: Boolean) {
    Text(
        text = dayNumber.toString(),
        style = MaterialTheme.typography.bodyMedium,
        color = when {
            isBooked -> BookedRed
            isBlocked -> BlockedOrange
            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .background(color, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

fun bookedDaysFromRanges(ranges: List<Pair<Long, Long>>): Set<Long> {
    val days = mutableSetOf<Long>()
    ranges.forEach { (start, end) ->
        var day = start
        while (day < end) {
            days.add(day)
            day++
        }
    }
    return days
}
