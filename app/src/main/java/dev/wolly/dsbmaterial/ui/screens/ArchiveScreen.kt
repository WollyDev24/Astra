@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.components.dpv

@Composable
fun ArchiveScreen(
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    onRemoveGroup: (List<SubstitutionEntry>) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingRemoval by remember { mutableStateOf<List<SubstitutionEntry>?>(null) }
    val pendingGroup = pendingRemoval
    if (pendingGroup != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.msg_delete_title)) },
            text = { Text(stringResource(R.string.msg_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveGroup(pendingGroup)
                    pendingRemoval = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.msg_no_substitutions), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        // Grouping the whole archive on every recomposition (e.g. toggling a setting)
        // re-visits every entry; cache it until the archive actually changes.
        val grouped = remember(entries) { entries.groupBy { it.day } }
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = dpv(16.dp, 20.dp),
                top = dpv(16.dp, 20.dp),
                end = dpv(16.dp, 20.dp),
                bottom = dpv(54.dp,56.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedButton(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth(), shape = CircleShape) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_open_calendar))
                }
            }
            grouped.forEach { (day, dayEntries) ->
                item(key = "day-$day") {
                    Text(day, style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                }
                val groups = groupSubstitutions(dayEntries)
                items(groups, key = { substitutionGroupKey(it) }) { group ->
                    val groupEntry = remember(group) { mergedGroupEntry(group) }
                    val periodLabel = remember(group) { mergedPeriodLabel(group) }
                    Card(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(modifier = Modifier.padding(dpv(16.dp, 28.dp)), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                SubstitutionTableRowContent(
                                    entry = groupEntry,
                                    isRoomFirst = isRoomFirst,
                                    period = periodLabel,
                                    singleLine = true
                                )
                            }
                            IconButton(onClick = { pendingRemoval = group }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_remove_entry), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}
