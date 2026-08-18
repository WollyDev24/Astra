@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.components.dpv
import dev.wolly.dsbmaterial.ui.components.isExpandedScreen
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import dev.wolly.dsbmaterial.ui.theme.springDefaultEffects
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayList(entries: List<SubstitutionEntry>, onDayClick: (String, Rect) -> Unit, selectedDay: String? = null, cardAlpha: Float = 1f, isRefreshing: Boolean = false, onRefresh: () -> Unit = {}, lastUpdated: Long? = null, isOffline: Boolean = false, modifier: Modifier = Modifier) {
    var filterQuery by remember { mutableStateOf("") }

    val allDayData = remember(entries) {
        val filtered = entries.filter { day ->
            val lowerDay = day.day.lowercase()
            !lowerDay.contains("samstag") && !lowerDay.contains("sonntag") &&
            !lowerDay.contains("saturday") && !lowerDay.contains("sunday")
        }
        val distinctDays = filtered.map { it.day }.distinct()
        val counts = mutableMapOf<String, Int>()
        for (entry in filtered) {
            counts[entry.day] = (counts[entry.day] ?: 0) + 1
        }
        val allEntriesByDay = filtered.groupBy { it.day }
        Triple(distinctDays, counts, allEntriesByDay)
    }
    val days = allDayData.first
    val dayCounts = allDayData.second
    val allEntriesByDay = allDayData.third
    val classesByDay = remember(allDayData) {
        allDayData.third.mapValues { (_, dayEntries) ->
            dayEntries.map { it.className }.filter { it.isNotEmpty() }.distinct()
        }
    }

    val filteredDays = remember(days, filterQuery, allEntriesByDay) {
        if (filterQuery.isBlank()) {
            days
        } else {
            val q = filterQuery.lowercase()
            days.filter { day ->
                allEntriesByDay[day]?.any { entry ->
                    entry.subject.lowercase().contains(q) ||
                    entry.room.lowercase().contains(q) ||
                    entry.art.lowercase().contains(q) ||
                    entry.text.lowercase().contains(q) ||
                    entry.lesson.lowercase().contains(q)
                } == true
            }
        }
    }

    val todayDayName = remember {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Montag"
            Calendar.TUESDAY -> "Dienstag"
            Calendar.WEDNESDAY -> "Mittwoch"
            Calendar.THURSDAY -> "Donnerstag"
            Calendar.FRIDAY -> "Freitag"
            Calendar.SATURDAY -> "Samstag"
            Calendar.SUNDAY -> "Sonntag"
            else -> ""
        }
    }

    val todayDateStr = remember {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        String.format("%02d.%02d.%04d", day, month, year)
    }

    val isToday = remember(days) {
        days.any { day ->
            val lower = day.lowercase()
            lower.startsWith(todayDayName.lowercase()) ||
            lower.contains(todayDateStr)
        }
    }

    val nextUpDay = remember(days, isToday, todayDayName, todayDateStr) {
        if (isToday) null
        else {
            val dayOrder = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
            val todayIndex = dayOrder.indexOfFirst { it.equals(todayDayName, ignoreCase = true) }
            if (todayIndex < 0) null
            else {
                val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
                days.sortedBy { day ->
                    val match = dateRegex.find(day)
                    if (match != null) {
                        val (d, m, y) = match.destructured
                        y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
                    } else {
                        val matchIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
                        if (matchIndex >= 0) {
                            val adjustedIndex = if (matchIndex < todayIndex) matchIndex + 7 else matchIndex
                            adjustedIndex.toLong()
                        } else Long.MAX_VALUE
                    }
                }.firstOrNull()
            }
        }
    }

    if (days.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.msg_no_substitutions), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        val isTablet = isExpandedScreen()
        val pad = remember(isTablet) { PaddingValues(if (isTablet) 20.dp else 20.dp) }
        val spacing = remember(isTablet) { if (isTablet) 16.dp else 24.dp }

        val pullRefreshState = rememberPullToRefreshState()

        if (isTablet) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                    )
                }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(280.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = pad,
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    if (filterQuery.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                FilterBar(filterQuery = filterQuery, onFilterChange = { filterQuery = it })
                                LastUpdatedCaption(lastUpdated = lastUpdated, isOffline = isOffline)
                            }
                        }
                    }
                    items(count = filteredDays.size, key = { filteredDays[it] }) { index ->
                        val day = filteredDays[index]
                        val isOrigin = isTablet && selectedDay != null && day == selectedDay
                        val cardBounds = remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
                        val isCurrentDay = isToday && day.lowercase().startsWith(todayDayName.lowercase())
                        val isNext = !isCurrentDay && nextUpDay != null && day == nextUpDay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isOrigin) cardAlpha else 1f)
                                .onGloballyPositioned { cardBounds.value = it.boundsInRoot() },
                            contentAlignment = Alignment.TopStart
                        ) {
                            DayCard(day, dayCounts[day] ?: 0, classes = classesByDay[day] ?: emptyList(), isToday = isCurrentDay, isNextUp = isNext) {
                                if (!isOrigin) onDayClick(day, cardBounds.value)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(48.dp)) }
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                    )
                }
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = pad, verticalArrangement = Arrangement.spacedBy(spacing)) {
                    item {
                        Column {
                            FilterBar(filterQuery = filterQuery, onFilterChange = { filterQuery = it })
                            LastUpdatedCaption(lastUpdated = lastUpdated, isOffline = isOffline)
                        }
                    }
                    columnItems(filteredDays, key = { it }) { day ->
                        val isCurrentDay = isToday && day.lowercase().startsWith(todayDayName.lowercase())
                        val isNext = !isCurrentDay && nextUpDay != null && day == nextUpDay
                        DayCard(day, dayCounts[day] ?: 0, classes = classesByDay[day] ?: emptyList(), isToday = isCurrentDay, isNextUp = isNext) { onDayClick(day, Rect(0f, 0f, 0f, 0f)) }
                    }
                    item { Spacer(Modifier.height(48.dp)) }
                }
            }
        }
    }
}

@Composable
fun FilterBar(filterQuery: String, onFilterChange: (String) -> Unit) {
    OutlinedTextField(
        value = filterQuery,
        onValueChange = onFilterChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.label_filter_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = {
            if (filterQuery.isNotEmpty()) {
                IconButton(onClick = { onFilterChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear_filter), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun LastUpdatedCaption(lastUpdated: Long?, isOffline: Boolean) {
    if (lastUpdated == null && !isOffline) return
    val stale = lastUpdated != null && System.currentTimeMillis() - lastUpdated > 24 * 60 * 60 * 1000L
    val color = when {
        isOffline -> MaterialTheme.colorScheme.error
        stale -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = when {
                isOffline -> stringResource(R.string.home_offline)
                stale && lastUpdated != null -> stringResource(R.string.home_last_updated, formatUpdatedTime(lastUpdated))
                lastUpdated != null -> stringResource(R.string.home_last_updated, formatUpdatedTime(lastUpdated))
                else -> stringResource(R.string.home_offline)
            },
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
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

@Composable
private fun DayCard(day: String, count: Int, classes: List<String> = emptyList(), isToday: Boolean = false, isNextUp: Boolean = false, onDayClick: (String) -> Unit) {
    val isTablet = isExpandedScreen()
    val appTypography = MaterialTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "daycard_press_scale"
    )
    val cardRadius by animateDpAsState(
        targetValue = if (isPressed) 20.dp else if (isToday) 48.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 380f
        ),
        label = "daycard_shape"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = springDefaultSpatial(),
        label = "daycard_elevation"
    )
    val cardColor by animateColorAsState(
        targetValue = when {
            isToday -> MaterialTheme.colorScheme.primaryContainer
            isNextUp -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = springDefaultEffects(),
        label = "daycard_color"
    )
    val headlineColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        isNextUp -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val chipBg = when {
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        isNextUp -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val chipFg = if (isNextUp) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        interactionSource = interactionSource,
        onClick = { }
    ) {
        Column(modifier = Modifier.padding(dpv(16.dp, 24.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = day, style = appTypography.headlineMediumEmphasized, color = headlineColor, modifier = Modifier.weight(1f))
                if (isToday) {
                    Surface(
                        shape = fullRoundedShape(),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.label_today),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else if (isNextUp) {
                    Surface(
                        shape = fullRoundedShape(),
                        color = MaterialTheme.colorScheme.secondary,
                    ) {
                        Text(
                            text = stringResource(R.string.label_next_up),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (count == 1) stringResource(R.string.format_substitutions_count_one, count)
                       else stringResource(R.string.format_substitutions_count_many, count),
                style = appTypography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (classes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val visibleClasses = classes.take(5)
                val overflowCount = classes.size - visibleClasses.size
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    visibleClasses.forEach { cls ->
                        Surface(
                            shape = fullRoundedShape(),
                            color = chipBg
                        ) {
                            Text(
                                text = cls,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = chipFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (overflowCount > 0) {
                        Surface(
                            shape = fullRoundedShape(),
                            color = chipBg.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "+$overflowCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = chipFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (isTablet) 16.dp else 24.dp))
            Button(
                onClick = { onDayClick(day) },
                modifier = Modifier.fillMaxWidth().height(if (isTablet) 48.dp else dpv(56.dp, 64.dp)),
                shape = CircleShape
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.action_view_substitutions), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SubstitutionViewer(
    day: String, 
    entries: List<SubstitutionEntry>, 
    isRoomFirst: Boolean, 
    isExpanded: Boolean,
    onShare: (String) -> Unit = {}
) {
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "expand_progress"
    )
    val headerFontSize = 22f + (36f - 22f) * expandProgress
    val containerPadding = lerp(12.dp, 8.dp, expandProgress)
    val textPaddingStart = lerp(16.dp, 12.dp, expandProgress)
    val textPaddingTop = lerp(16.dp, 8.dp, expandProgress)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = containerPadding)) {
        Row(
            modifier = Modifier.padding(start = textPaddingStart, top = textPaddingTop, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.headlineLargeEmphasized.copy(
                    fontSize = headerFontSize.sp,
                    lineHeight = (headerFontSize * 1.2f).sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onShare(day) }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share), tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Row(modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TableHeaderCell(stringResource(R.string.label_period_short), 1f)
                TableHeaderCell(stringResource(R.string.label_subject_short), 1.8f)
                TableHeaderCell(stringResource(R.string.label_room), 1.4f)
                TableHeaderCell(stringResource(R.string.label_type), 2f)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        val grouped = remember(entries) { groupSubstitutions(entries) }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f), 
            contentPadding = PaddingValues(bottom = 120.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            columnItems(grouped, key = ::substitutionGroupKey) { group ->
                SubstitutionTableRowGroup(group, isRoomFirst)
            }
        }
    }
}

@Composable
fun TabletSubstitutionPopup(
    selectedDay: String,
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    cardRect: Rect,
    onDismissStart: () -> Unit,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val popAlpha = remember(selectedDay) { Animatable(0f) }
    val popScale = remember(selectedDay) { Animatable(0.85f) }

    LaunchedEffect(selectedDay) {
        launch { popAlpha.animateTo(1f, springDefaultEffects()) }
        popScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }

    fun dismiss() {
        scope.launch {
            onDismissStart()
            launch { popAlpha.animateTo(0f, springDefaultEffects()) }
            popScale.animateTo(0.85f, springDefaultSpatial())
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (popAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * popAlpha.value))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { dismiss() }
                    )
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                    alpha = popAlpha.value
                }
                .fillMaxWidth(0.7f)
                .widthIn(max = 480.dp)
                .heightIn(max = 400.dp)
                .clickable(enabled = false) {},
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SubstitutionViewer(
                    day = selectedDay,
                    entries = entries,
                    isRoomFirst = isRoomFirst,
                    isExpanded = true,
                    onShare = { day ->
                        dismiss()
                        onShare(day)
                    }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(fullRoundedShape())
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { dismiss() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text, 
        modifier = Modifier.weight(weight), 
        style = MaterialTheme.typography.labelMedium, 
        fontWeight = FontWeight.ExtraBold, 
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        letterSpacing = 1.sp
    )
}

@Composable
fun SubstitutionTableRowGroup(
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean
) {
    if (entries.isEmpty()) return
    val groupEntry = remember(entries) { mergedGroupEntry(entries) }
    val periodLabel = remember(entries) { mergedPeriodLabel(entries) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        )
    ) {
        SubstitutionTableRowContent(
            entry = groupEntry,
            isRoomFirst = isRoomFirst,
            period = periodLabel
        )
    }
}

fun mergedGroupEntry(entries: List<SubstitutionEntry>): SubstitutionEntry {
    val mergedRoom = entries.map { it.room }.filter { it.isNotEmpty() }.distinct().joinToString(" / ")
    val mergedText = entries.map { it.text }.filter { it.isNotEmpty() }.distinct().joinToString(" · ")
    return entries.first().copy(room = mergedRoom, text = mergedText)
}

@Composable
fun SubstitutionTableRowContent(
    entry: SubstitutionEntry,
    isRoomFirst: Boolean,
    period: String = entry.lesson,
    singleLine: Boolean = false
) {
    val roomDisplay = if (isRoomFirst) entry.room else entry.art
    val typeDisplay = if (isRoomFirst) entry.art else entry.room

    val periodStyle = MaterialTheme.typography.titleMedium.copy(fontSize = if (singleLine) 13.sp else 15.sp)
    val subjectStyle = MaterialTheme.typography.titleMedium.copy(fontSize = if (singleLine) 13.sp else 15.sp)
    val roomStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = if (singleLine) 13.sp else 15.sp)
    val periodWeight = if (singleLine) 1.1f else 1f
    val subjectWeight = if (singleLine) 2.0f else 1.8f
    val roomWeight = if (singleLine) 1.3f else 1.4f
    val typeWeight = if (singleLine) 2.1f else 2f
    val maxLines = if (singleLine) 1 else 2

    Column(modifier = Modifier.padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell(period, periodWeight, fontWeight = FontWeight.ExtraBold, style = periodStyle, maxLines = maxLines)
            TableCell(entry.subject, subjectWeight, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, style = subjectStyle, maxLines = maxLines)
            TableCell(roomDisplay.ifEmpty { "—" }, roomWeight, fontWeight = FontWeight.Bold, style = roomStyle, maxLines = maxLines)
            val defaultTypeColor = MaterialTheme.colorScheme.secondary
            val typeColor = remember(typeDisplay, defaultTypeColor) {
                val lower = typeDisplay.lowercase()
                when {
                    lower.contains("entfall") -> Color(0xFFD32F2F)
                    lower.contains("vertretung") -> Color(0xFFF57C00)
                    lower.contains("verlegung") || lower.contains("verschiebung") -> Color(0xFF1976D2)
                    lower.contains("eigenvertretung") -> Color(0xFF7B1FA2)
                    lower.contains("betreuung") -> Color(0xFF388E3C)
                    lower.contains("raumänderung") || lower.contains("raum") -> Color(0xFF00838F)
                    else -> defaultTypeColor
                }
            }
            val typeBgColor = typeColor.copy(alpha = 0.12f)
            Box(
                modifier = Modifier
                    .weight(typeWeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(typeBgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = typeDisplay,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = typeColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (entry.className.isNotEmpty()) {
            Text(
                text = entry.className,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        if (entry.text.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String, 
    weight: Float, 
    fontWeight: FontWeight = FontWeight.Normal, 
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = 2
    ) {
    Text(
        text = text, 
        modifier = Modifier.weight(weight), 
        style = style, 
        fontWeight = fontWeight, 
        color = color, 
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis
    )
}

fun groupSubstitutions(entries: List<SubstitutionEntry>): List<List<SubstitutionEntry>> =
    entries.groupBy { entry ->
        entry.day + "|" + entry.art + "|" + entry.className + "|" + entry.subject
    }.values.toList()

fun substitutionGroupKey(group: List<SubstitutionEntry>): String =
    group.first().let { it.day + "|" + it.art + "|" + it.className + "|" + it.subject } + "|" + mergedPeriodLabel(group)

fun mergedPeriodLabel(entries: List<SubstitutionEntry>): String {
    if (entries.size == 1) return entries.first().lesson
    val nums = entries.flatMap { periodNumbers(it.lesson) }.distinct().sorted()
    if (nums.isEmpty()) return entries.joinToString(", ") { it.lesson }
    val parts = mutableListOf<String>()
    var start = 0
    while (start < nums.size) {
        var end = start
        while (end + 1 < nums.size && nums[end + 1] == nums[end] + 1) end++
        parts.add(if (end > start) "${nums[start]}–${nums[end]}" else nums[start].toString())
        start = end + 1
    }
    return parts.joinToString(", ")
}

private val periodNumberRegex = Regex("\\d+")

private fun periodNumbers(lesson: String): List<Int> =
    periodNumberRegex.findAll(lesson).map { it.value.toInt() }.toList()

fun buildShareText(day: String, entries: List<SubstitutionEntry>, isRoomFirst: Boolean): String {
    val sb = StringBuilder()
    sb.appendLine("📚 $day")
    sb.appendLine("─".repeat(20))
    for (group in groupSubstitutions(entries)) {
        val entry = group.first()
        val roomDisplay = if (isRoomFirst) entry.room else entry.art
        val typeDisplay = if (isRoomFirst) entry.art else entry.room
        sb.appendLine("${mergedPeriodLabel(group)} | ${entry.subject} | $roomDisplay | $typeDisplay")
        val mergedText = group.map { it.text }.filter { it.isNotEmpty() }.distinct().joinToString(" · ")
        if (mergedText.isNotEmpty()) {
            sb.appendLine("  → $mergedText")
        }
    }
    sb.appendLine()
    sb.appendLine("Astra: https://github.com/wollydev24/DSBmaterial")
    return sb.toString()
}
