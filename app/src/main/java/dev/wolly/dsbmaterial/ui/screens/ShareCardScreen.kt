@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.ui.text.ExperimentalTextApi::class
)
package dev.wolly.dsbmaterial.ui.screens

import android.graphics.Bitmap
import android.os.Build
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.components.dpv
import dev.wolly.dsbmaterial.ui.theme.SeedPalettes
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareCardScreen(
    day: String,
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    themeIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var useMaterialYou by remember { mutableStateOf(false) }
    var presetIndex by remember { mutableStateOf(themeIndex.coerceIn(SeedPalettes.indices)) }
    var isSharing by remember { mutableStateOf(false) }

    val scheme =
        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            SeedPalettes[presetIndex.coerceIn(SeedPalettes.indices)].scheme(dark = false)
        }

    val fontFamily =
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 100f))
            )
        )

    fun shareImage() {
        if (isSharing) return
        isSharing = true
        scope.launch {
            try {
                val image = graphicsLayer.toImageBitmap().asAndroidBitmap()
                val uri = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, "substitutions_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        image.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, day)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
            } catch (_: Exception) {
            } finally {
                isSharing = false
            }
        }
    }

    fun shareText() {
        val shareText = buildShareText(day, entries, isRoomFirst)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, day)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(R.string.label_share_card),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                ShareSectionTitle(stringResource(R.string.label_preview), Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 16.dp)
                ) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val cardWidth = 360.dp
                        val scale = (maxWidth / cardWidth).coerceAtMost(1f)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .width(cardWidth)
                                    .scale(scale)
                            ) {
                                ShareCardCapture(graphicsLayer, day, entries, isRoomFirst, scheme, fontFamily)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(Modifier.padding(horizontal = 24.dp)) {
                ShareSectionTitle(stringResource(R.string.label_colors))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useMaterialYou,
                        onClick = { useMaterialYou = false },
                        label = { Text(stringResource(R.string.label_preset)) }
                    )
                    FilterChip(
                        selected = useMaterialYou,
                        onClick = { useMaterialYou = true },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        label = { Text(stringResource(R.string.label_material_you)) }
                    )
                }

                if (!useMaterialYou) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SeedPalettes.forEachIndexed { index, palette ->
                            val isSelected = index == presetIndex
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.scheme(dark = false).primary)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { presetIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { shareImage() },
                enabled = !isSharing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dpv(56.dp, 64.dp)),
                shape = fullRoundedShape()
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.action_share_image),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            TextButton(
                onClick = { shareText() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.action_share_text),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ShareSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun ShareCardCapture(
    graphicsLayer: GraphicsLayer,
    day: String,
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    scheme: ColorScheme,
    fontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
    ) {
        ShareCardContent(day, entries, isRoomFirst, scheme, fontFamily)
    }
}

@Composable
private fun ShareCardContent(
    day: String,
    entries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    scheme: ColorScheme,
    fontFamily: FontFamily
) {
    val grouped = remember(entries) { groupSubstitutions(entries) }
    val showClass = remember(entries) {
        entries.map { it.className }.filter { it.isNotEmpty() }.distinct().size > 1
    }

    Surface(
        color = scheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.width(360.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                        color = scheme.primary
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = day,
                        style = TextStyle(fontFamily = fontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
                        color = scheme.onSurface
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = scheme.primary) {
                    Text(
                        text = entries.size.toString(),
                        style = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = scheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (grouped.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_entries),
                    style = TextStyle(fontFamily = fontFamily, fontSize = 13.sp, lineHeight = 18.sp),
                    color = scheme.onSurfaceVariant
                )
            } else {
                grouped.forEachIndexed { index, group ->
                    ShareCardRow(group, isRoomFirst, showClass, scheme, fontFamily)
                    if (index < grouped.lastIndex) Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            Row {
                Text(
                    text = stringResource(R.string.app_name),
                    style = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.share_card_footer),
                    style = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShareCardRow(
    group: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    showClass: Boolean,
    scheme: ColorScheme,
    fontFamily: FontFamily
) {
    val mergedRoom = group.map { it.room }.filter { it.isNotEmpty() }.distinct().joinToString(" / ")
    val mergedText = group.map { it.text }.filter { it.isNotEmpty() }.distinct().joinToString(" · ")
    val entry = group.first().copy(room = mergedRoom, text = mergedText)
    val roomDisplay = if (isRoomFirst) entry.room else entry.art
    val typeDisplay = if (isRoomFirst) entry.art else entry.room

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mergedPeriodLabel(group),
                style = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = scheme.onPrimaryContainer,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showClass && entry.className.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(scheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.className,
                            style = TextStyle(fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = scheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = entry.subject,
                    style = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val info = listOfNotNull(
                    roomDisplay.ifEmpty { null },
                    typeDisplay.ifEmpty { null }
                ).joinToString(" · ")
                if (info.isNotEmpty()) {
                    Text(
                        text = info,
                        style = TextStyle(fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
            if (entry.text.isNotEmpty()) {
                Text(
                    text = entry.text,
                    style = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, lineHeight = 16.sp),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
