@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import dev.wolly.dsbmaterial.R
import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import dev.wolly.dsbmaterial.BuildConfig
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.UiState
import dev.wolly.dsbmaterial.ui.components.*
import dev.wolly.dsbmaterial.ui.theme.springDefaultEffects
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CalendarView(
    archiveDates: List<Pair<String, Int>>,
    selectedDay: String?,
    isRoomFirst: Boolean,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")

    val monthNamesDe = listOf("Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember")

    val withDates = mutableListOf<Pair<Triple<Int, Int, Int>, Pair<String, Int>>>()
    val withoutDates = mutableListOf<Pair<String, Int>>()

    archiveDates.forEach { (day, count) ->
        val match = dateRegex.find(day)
        if (match != null) {
            val (d, m, y) = match.destructured
            withDates.add(Triple(m.toInt(), d.toInt(), y.toInt()) to (day to count))
        } else {
            withoutDates.add(day to count)
        }
    }

    val groupedByMonth = withDates.groupBy { it.first.first }.toSortedMap()

    if (archiveDates.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.label_no_calendar_data), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            groupedByMonth.forEach { (month, entries) ->
                item {
                    val monthName = monthNamesDe.getOrElse(month - 1) { "" }
                    val year = entries.first().first.third
                    Text(
                        text = "$monthName $year",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
                items(entries.map { it.second }, key = { (day, _) -> day }) { (day, count) ->
                    CalendarDayCard(day, count, isSelected = day == selectedDay, onClick = { onDayClick(day) })
                }
            }

            if (withoutDates.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_archive),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
                items(withoutDates, key = { (day, _) -> day }) { (day, count) ->
                    CalendarDayCard(day, count, isSelected = day == selectedDay, onClick = { onDayClick(day) })
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun CalendarDayCard(day: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = springDefaultEffects(),
        label = "cal_day_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = springDefaultEffects(),
        label = "cal_day_content_color"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(day, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = contentColor)
                Text(
                    stringResource(R.string.format_substitutions_count_many, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

