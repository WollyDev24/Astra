package dev.wolly.dsbmaterial

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.ColorFilter
import androidx.glance.color.ColorProvider
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.screens.groupSubstitutions
import dev.wolly.dsbmaterial.ui.screens.mergedGroupEntry
import dev.wolly.dsbmaterial.ui.screens.mergedPeriodLabel
import dev.wolly.dsbmaterial.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

private data class WidgetColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHighest: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val headerBg: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

private fun computeThemeColors(context: Context, themeIndex: Int, dynamicColor: Boolean): WidgetColors {
    val isDark = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return WidgetColors(
            primary = scheme.primary,
            onPrimary = scheme.onPrimary,
            background = scheme.background,
            surface = scheme.surface,
            surfaceContainerLow = scheme.surfaceContainerLow,
            surfaceContainerHighest = scheme.surfaceContainerHighest,
            primaryContainer = scheme.primaryContainer,
            onPrimaryContainer = scheme.onPrimaryContainer,
            secondary = scheme.secondary,
            headerBg = scheme.surfaceVariant,
            onSurface = scheme.onSurface,
            onSurfaceVariant = scheme.onSurfaceVariant
        )
    }

    val scheme = SeedPalettes.getOrElse(themeIndex) { SeedPalettes[0] }.scheme(dark = isDark)
    return WidgetColors(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        background = scheme.background,
        surface = scheme.surface,
        surfaceContainerLow = scheme.surfaceContainerLow,
        surfaceContainerHighest = scheme.surfaceContainerHighest,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        secondary = scheme.secondary,
        headerBg = scheme.surfaceVariant,
        onSurface = scheme.onSurface,
        onSurfaceVariant = scheme.onSurfaceVariant
    )
}

class DSBWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataStoreManager = DataStoreManager(context)
        val isRoomFirst = dataStoreManager.swapDataFlow.first()
        val themeIndex = dataStoreManager.themeIndexFlow.first()
        val dynamicColor = dataStoreManager.dynamicColorFlow.first()
        val colors = computeThemeColors(context, themeIndex, dynamicColor)
        val result = loadEntries(context)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    entries = result.entries,
                    headerText = result.headerText,
                    isRoomFirst = isRoomFirst,
                    isToday = result.isToday,
                    colors = colors,
                    context = context
                )
            }
        }
    }

    private data class WidgetResult(
        val entries: List<SubstitutionEntry>,
        val headerText: String,
        val isToday: Boolean
    )

    private suspend fun loadEntries(context: Context): WidgetResult = withContext(Dispatchers.IO) {
        try {
            val dataStoreManager = DataStoreManager(context)
            val archiveJson = dataStoreManager.archiveFlow.first() ?: return@withContext WidgetResult(emptyList(), "", false)
            if (archiveJson.isEmpty()) return@withContext WidgetResult(emptyList(), "", false)

            val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
            val allEntries: List<SubstitutionEntry> = Gson().fromJson(archiveJson, type)

            val todayDayName = getTodayDayName()
            val todayDateStr = getTodayDateStr()

            val todayEntries = allEntries.filter { entry ->
                val lower = entry.day.lowercase()
                (todayDayName.isNotEmpty() && lower.startsWith(todayDayName.lowercase())) || lower.contains(todayDateStr)
            }

            if (todayEntries.isNotEmpty()) {
                return@withContext WidgetResult(todayEntries, todayDayName.ifEmpty { allEntries.firstOrNull()?.day ?: "" }, true)
            }

            val dayOrder = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag")
            val todayIndex = if (todayDayName.isNotEmpty()) {
                dayOrder.indexOfFirst { it.equals(todayDayName, ignoreCase = true) }
            } else -1

            val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
            val upcomingDay = allEntries
                .map { it.day }
                .distinct()
                .filter { day ->
                    val lower = day.lowercase()
                    !lower.contains("samstag") && !lower.contains("sonntag") &&
                    !lower.contains("saturday") && !lower.contains("sunday")
                }
                .sortedBy { day ->
                    val match = dateRegex.find(day)
                    if (match != null) {
                        val (d, m, y) = match.destructured
                        y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
                    } else {
                        val matchIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
                        if (matchIndex >= 0) {
                            1000L + matchIndex
                        } else {
                            2000L
                        }
                    }
                }
                .firstOrNull { day ->
                    if (todayIndex < 0) {
                        true
                    } else {
                        val dayIndex = dayOrder.indexOfFirst { day.lowercase().startsWith(it.lowercase()) }
                        if (dayIndex >= 0) dayIndex > todayIndex else true
                    }
                }

            if (upcomingDay != null) {
                val upcomingEntries = allEntries.filter { it.day == upcomingDay }
                val header = upcomingDay.replace(Regex(",?\\s*den\\s+\\d{2}\\.\\d{2}\\.\\d{4}"), "").trim()
                return@withContext WidgetResult(upcomingEntries, header, false)
            }

            WidgetResult(emptyList(), todayDayName, false)
        } catch (e: Exception) {
            WidgetResult(emptyList(), getTodayDayName(), false)
        }
    }

    private fun getTodayDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Montag"
            Calendar.TUESDAY -> "Dienstag"
            Calendar.WEDNESDAY -> "Mittwoch"
            Calendar.THURSDAY -> "Donnerstag"
            Calendar.FRIDAY -> "Freitag"
            else -> ""
        }
    }

    private fun getTodayDateStr(): String {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        return "%02d.%02d.%04d".format(day, month, year)
    }
}

@Composable
private fun WidgetContent(
    entries: List<SubstitutionEntry>,
    headerText: String,
    isRoomFirst: Boolean,
    isToday: Boolean,
    colors: WidgetColors,
    context: Context
) {
    val size = LocalSize.current
    if (size.height <= 150.dp || size.width <= 150.dp) {
        CompactWidget(entries, headerText, isRoomFirst, isToday, colors, context)
        return
    }
    FullWidget(entries, headerText, isRoomFirst, isToday, colors, context, size.height)
}

@Composable
private fun FullWidget(
    entries: List<SubstitutionEntry>,
    headerText: String,
    isRoomFirst: Boolean,
    isToday: Boolean,
    colors: WidgetColors,
    context: Context,
    height: Dp
) {
    val textColor = ColorProvider(day = colors.onSurface, night = colors.onSurface)
    val secondaryTextColor = ColorProvider(day = colors.onSurfaceVariant, night = colors.onSurfaceVariant)
    val primaryTextColor = ColorProvider(day = colors.primary, night = colors.primary)
    val onPrimaryColor = ColorProvider(day = colors.onPrimary, night = colors.onPrimary)
    val onPrimaryContainerColor = ColorProvider(day = colors.onPrimaryContainer, night = colors.onPrimaryContainer)

    val titleSize = 22.sp
    val countSize = 13.sp
    val labelSize = 11.sp
    val entrySize = 15.sp
    val typeSize = 10.sp
    val padH = 18.dp
    val padV = 14.dp
    val periodW = 34.dp
    val roomW = 54.dp
    val typeW = 88.dp
    val showClass = entries.map { it.className }.filter { it.isNotEmpty() }.distinct().size > 1

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = colors.background, night = colors.background))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = padH, vertical = padV)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_calendar),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(primaryTextColor),
                    modifier = GlanceModifier.size(24.dp)
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Text(
                    text = headerText,
                    style = TextStyle(
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (isToday && entries.isNotEmpty()) {
                    Text(
                        text = context.getString(R.string.label_today),
                        style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = onPrimaryColor),
                        modifier = GlanceModifier
                            .background(ColorProvider(day = colors.primary, night = colors.primary))
                            .cornerRadius(14.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                if (entries.isNotEmpty()) {
                    Text(
                        text = "${groupSubstitutions(entries).size}",
                        style = TextStyle(fontSize = countSize, fontWeight = FontWeight.Bold, color = onPrimaryContainerColor),
                        modifier = GlanceModifier
                            .background(ColorProvider(day = colors.primaryContainer, night = colors.primaryContainer))
                            .cornerRadius(14.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (entries.isEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_calendar),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(secondaryTextColor),
                        modifier = GlanceModifier.size(36.dp)
                    )
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Text(
                        text = context.getString(R.string.widget_no_substitutions),
                        style = TextStyle(fontSize = entrySize, color = secondaryTextColor)
                    )
                }
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(day = colors.headerBg, night = colors.headerBg))
                        .cornerRadius(12.dp)
                        .padding(vertical = 6.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = context.getString(R.string.label_period_short), style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.width(periodW))
                    Text(text = context.getString(R.string.label_subject_short), style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.defaultWeight())
                    Text(text = context.getString(R.string.label_room), style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.width(roomW))
                    Text(text = context.getString(R.string.label_type), style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = secondaryTextColor), modifier = GlanceModifier.width(typeW))
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                val groups = groupSubstitutions(entries)
                val maxRows = ((height - 96.dp) / 30.dp).toInt().coerceIn(2, 6)
                groups.take(maxRows).forEach { group ->
                    val entry = mergedGroupEntry(group)
                    val roomDisplay = if (isRoomFirst) entry.room else entry.art
                    val typeDisplay = if (isRoomFirst) entry.art else entry.room

                    val typeColor = when {
                        typeDisplay.lowercase().contains("entfall") -> Color(0xFFD32F2F)
                        typeDisplay.lowercase().contains("vertretung") -> Color(0xFFF57C00)
                        typeDisplay.lowercase().contains("verlegung") || typeDisplay.lowercase().contains("verschiebung") -> Color(0xFF1976D2)
                        typeDisplay.lowercase().contains("eigenvertretung") -> Color(0xFF7B1FA2)
                        typeDisplay.lowercase().contains("betreuung") -> Color(0xFF388E3C)
                        typeDisplay.lowercase().contains("raumänderung") || typeDisplay.lowercase().contains("raum") -> Color(0xFF00838F)
                        else -> colors.secondary
                    }

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(day = colors.surfaceContainerLow, night = colors.surfaceContainerLow))
                            .cornerRadius(12.dp)
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(periodW)
                                .cornerRadius(8.dp)
                                .background(ColorProvider(day = colors.surfaceContainerHighest, night = colors.surfaceContainerHighest))
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = mergedPeriodLabel(group), style = TextStyle(fontSize = entrySize, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                        }
                        Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            if (showClass && entry.className.isNotEmpty()) {
                                Box(
                                    modifier = GlanceModifier
                                        .cornerRadius(6.dp)
                                        .background(ColorProvider(day = colors.headerBg, night = colors.headerBg))
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = entry.className, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryTextColor), maxLines = 1)
                                }
                                Spacer(modifier = GlanceModifier.width(4.dp))
                            }
                            Text(text = entry.subject, style = TextStyle(fontSize = entrySize, fontWeight = FontWeight.Bold, color = primaryTextColor), modifier = GlanceModifier.defaultWeight())
                        }
                        Text(text = roomDisplay.ifEmpty { "—" }, style = TextStyle(fontSize = entrySize, fontWeight = FontWeight.Bold, color = textColor), modifier = GlanceModifier.width(roomW))
                        Box(
                            modifier = GlanceModifier
                                .width(typeW)
                                .cornerRadius(8.dp)
                                .background(ColorProvider(day = typeColor.copy(alpha = 0.12f), night = typeColor.copy(alpha = 0.12f)))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = typeDisplay, style = TextStyle(fontSize = typeSize, fontWeight = FontWeight.Bold, color = ColorProvider(day = typeColor, night = typeColor)), maxLines = 1)
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))
                }

                if (groups.size > maxRows) {
                    Box(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = context.getString(R.string.widget_more, groups.size - maxRows),
                            style = TextStyle(fontSize = labelSize, fontWeight = FontWeight.Bold, color = primaryTextColor),
                            modifier = GlanceModifier
                                .background(ColorProvider(day = colors.surfaceContainerHighest, night = colors.surfaceContainerHighest))
                                .cornerRadius(16.dp)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWidget(
    entries: List<SubstitutionEntry>,
    headerText: String,
    isRoomFirst: Boolean,
    isToday: Boolean,
    colors: WidgetColors,
    context: Context
) {
    val textColor = ColorProvider(day = colors.onSurface, night = colors.onSurface)
    val secondaryTextColor = ColorProvider(day = colors.onSurfaceVariant, night = colors.onSurfaceVariant)
    val primaryTextColor = ColorProvider(day = colors.primary, night = colors.primary)
    val onPrimaryContainerColor = ColorProvider(day = colors.onPrimaryContainer, night = colors.onPrimaryContainer)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = colors.background, night = colors.background))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerText,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (entries.isNotEmpty()) {
                    Text(
                        text = "${groupSubstitutions(entries).size}",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = onPrimaryContainerColor),
                        modifier = GlanceModifier
                            .background(ColorProvider(day = colors.primaryContainer, night = colors.primaryContainer))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            if (entries.isEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_calendar),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(secondaryTextColor),
                        modifier = GlanceModifier.size(28.dp)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = context.getString(R.string.widget_no_substitutions),
                        style = TextStyle(fontSize = 13.sp, color = secondaryTextColor)
                    )
                }
            } else {
                val groups = groupSubstitutions(entries)
                val maxRows = ((LocalSize.current.height - 46.dp) / 26.dp).toInt().coerceIn(1, 3)
                groups.take(maxRows).forEach { group ->
                    val entry = mergedGroupEntry(group)
                    val typeDisplay = if (isRoomFirst) entry.art else entry.room

                    val typeColor = when {
                        typeDisplay.lowercase().contains("entfall") -> Color(0xFFD32F2F)
                        typeDisplay.lowercase().contains("vertretung") -> Color(0xFFF57C00)
                        typeDisplay.lowercase().contains("verlegung") || typeDisplay.lowercase().contains("verschiebung") -> Color(0xFF1976D2)
                        typeDisplay.lowercase().contains("eigenvertretung") -> Color(0xFF7B1FA2)
                        typeDisplay.lowercase().contains("betreuung") -> Color(0xFF388E3C)
                        typeDisplay.lowercase().contains("raumänderung") || typeDisplay.lowercase().contains("raum") -> Color(0xFF00838F)
                        else -> colors.secondary
                    }

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(day = colors.surfaceContainerLow, night = colors.surfaceContainerLow))
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mergedPeriodLabel(group),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = secondaryTextColor),
                            modifier = GlanceModifier.width(28.dp)
                        )
                        Text(
                            text = entry.subject,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryTextColor),
                            modifier = GlanceModifier.defaultWeight(),
                            maxLines = 1
                        )
                        Text(
                            text = typeDisplay,
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(day = typeColor, night = typeColor)),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
        }
    }
}