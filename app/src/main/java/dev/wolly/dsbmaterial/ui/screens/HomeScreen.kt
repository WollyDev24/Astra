@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import java.util.Calendar

@Composable
fun HomeScreen(
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    isRefreshing: Boolean,
    hasError: Boolean,
    lastUpdated: Long?,
    isOffline: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDayTap: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val schoolEntries = remember(entries) { entries.filter { isSchoolDay(it.day) } }
    val today = remember(schoolEntries) { todayInfo(schoolEntries) }
    val todayDate = remember { todayDateString() }
    val todayEntries = remember(schoolEntries, today) {
        schoolEntries.filter { isTodayDay(it.day, today.dayName, todayDate) }
    }
    val nextUpDay = remember(schoolEntries, today) {
        nextUp(schoolEntries, today.dayName, todayDate)
    }
    val nextUpEntries = remember(schoolEntries, nextUpDay) {
        if (nextUpDay == null) emptyList() else schoolEntries.filter { it.day == nextUpDay }
    }
    val weekDays = remember(schoolEntries) { currentWeekDays(schoolEntries) }

    val dayEntries = if (todayEntries.isNotEmpty()) todayEntries else nextUpEntries
    val isToday = todayEntries.isNotEmpty()
    val dayLabel = dayEntries.firstOrNull()?.day ?: today.dayName
    val grouped = remember(dayEntries) { groupSubstitutions(dayEntries) }

    if (dayEntries.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.home_no_entries),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { if (hasError) onRetry() else onRefresh() },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(if (hasError) stringResource(R.string.home_retry) else stringResource(R.string.action_refresh))
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HomeDayHeader(
                dayLabel = dayLabel,
                isToday = isToday,
                count = dayEntries.size,
                isRefreshing = isRefreshing,
                lastUpdated = lastUpdated,
                isOffline = isOffline,
                onRefresh = onRefresh
            )
        }

        item {
            WeekOverviewCard(
                weekDays = weekDays,
                onDayTap = onDayTap
            )
        }

        items(grouped.size, key = { index -> substitutionGroupKey(grouped[index]) }) { index ->
            SubstitutionTableRowGroup(grouped[index], isRoomFirst)
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun HomeDayHeader(
    dayLabel: String,
    isToday: Boolean,
    count: Int,
    isRefreshing: Boolean,
    lastUpdated: Long?,
    isOffline: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (isToday) stringResource(R.string.label_today)
                                   else stringResource(R.string.label_next_up),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (count == 1) stringResource(R.string.format_substitutions_count_one, count)
                               else stringResource(R.string.format_substitutions_count_many, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    lastUpdated?.let { timestamp ->
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOffline) {
                                Text(
                                    text = stringResource(R.string.home_offline),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = stringResource(R.string.home_last_updated, formatUpdatedTime(timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

private val updatedTimeFormat: java.text.DateFormat by lazy {
    java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT
    )
}

private fun formatUpdatedTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    return updatedTimeFormat.format(date)
}

private data class TodayInfo(val count: Int, val dayName: String)

private data class WeekDay(
    val dayName: String,
    val shortName: String,
    val date: String,
    val count: Int,
    val isToday: Boolean
)

@Composable
private fun WeekOverviewCard(
    weekDays: List<WeekDay>,
    onDayTap: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_week_overview),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                weekDays.forEach { day ->
                    WeekDayChip(
                        day = day,
                        modifier = Modifier.weight(1f),
                        onClick = { onDayTap(day.dayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayChip(
    day: WeekDay,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary
        day.count > 0 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when {
        day.isToday -> MaterialTheme.colorScheme.onPrimary
        day.count > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.shortName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (day.count == 0) "–" else day.count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (day.count == 0) contentColor.copy(alpha = 0.5f) else contentColor
            )
        }
    }
}

private fun currentWeekDays(schoolEntries: List<SubstitutionEntry>): List<WeekDay> {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val dayNames = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag")
    val shortNames = arrayOf("Mo", "Di", "Mi", "Do", "Fr")
    val today = todayDateString()
    return dayNames.indices.map { i ->
        val dayName = dayNames[i]
        val date = String.format(
            "%02d.%02d.%04d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        val count = schoolEntries.count { entry ->
            val lower = entry.day.lowercase()
            lower.startsWith(dayName.lowercase()) || lower.contains(date)
        }
        cal.add(Calendar.DAY_OF_MONTH, 1)
        WeekDay(dayName, shortNames[i], date, count, isToday = date == today)
    }
}

private fun isSchoolDay(day: String): Boolean {
    val lower = day.lowercase()
    return !lower.contains("samstag") && !lower.contains("sonntag") &&
        !lower.contains("saturday") && !lower.contains("sunday")
}

private fun todayInfo(entries: List<SubstitutionEntry>): TodayInfo {
    val cal = Calendar.getInstance()
    val dayNames = arrayOf("Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag")
    val dayName = dayNames[(cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) % 7]
    val date = todayDateString()
    val count = entries.count { entry ->
        val lower = entry.day.lowercase()
        lower.startsWith(dayName.lowercase()) || lower.contains(date)
    }
    return TodayInfo(count, dayName)
}

private fun todayDateString(): String {
    val cal = Calendar.getInstance()
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH) + 1
    val year = cal.get(Calendar.YEAR)
    return String.format("%02d.%02d.%04d", day, month, year)
}

private fun isTodayDay(day: String, todayDayName: String, todayDate: String): Boolean {
    val lower = day.lowercase()
    return lower.startsWith(todayDayName.lowercase()) || lower.contains(todayDate)
}

private fun nextUp(
    entries: List<SubstitutionEntry>,
    todayDayName: String,
    todayDate: String
): String? {
    val days = entries.map { it.day }.distinct()
    val others = days.filter { !isTodayDay(it, todayDayName, todayDate) }
    if (others.isEmpty()) return null
    val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
    val dayOrder = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    val todayIndex = dayOrder.indexOfFirst { it.equals(todayDayName, ignoreCase = true) }
    return others.minWithOrNull(compareBy { day ->
        val match = dateRegex.find(day)
        if (match != null) {
            val (d, m, y) = match.destructured
            y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
        } else {
            val matchIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
            if (matchIndex >= 0) {
                val adjusted = if (matchIndex < todayIndex) matchIndex + 7 else matchIndex
                adjusted.toLong()
            } else Long.MAX_VALUE
        }
    })
}
