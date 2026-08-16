@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.components.*
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    onSkip: () -> Unit,
    customServerUrl: String? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showCustomServer by remember { mutableStateOf(false) }
    var customUrl by remember(customServerUrl) { mutableStateOf(customServerUrl ?: "") }
    val scrollState = rememberScrollState()

    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val iconSize by animateDpAsState(
        targetValue = if (isKeyboardOpen) 60.dp else 100.dp,
        animationSpec = springDefaultSpatial(),
        label = "setup_icon_size"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = dpv(24.dp, 40.dp), vertical = dpv(40.dp, 56.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MorphingIconBackground(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(iconSize)
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize * 0.5f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.title_setup),
                style = MaterialTheme.typography.headlineLargeEmphasized,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.desc_setup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.label_username)) },
                modifier = Modifier.fillMaxWidth(),
                shape = fullRoundedShape(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            PasswordShapeField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { showCustomServer = !showCustomServer }) {
                Icon(
                    if (showCustomServer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (customServerUrl != null) "Custom Server: ${customServerUrl.removePrefix("http://").removePrefix("https://")}"
                    else "Connect to custom server"
                )
            }
            if (showCustomServer) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = {
                        customUrl = it
                        viewModel.setCustomServerUrl(it)
                    },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://localhost:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = fullRoundedShape(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (customUrl.isNotBlank()) viewModel.setCustomServerUrl(customUrl)
                    viewModel.loginFromSetup(username, password)
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = CircleShape,
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = viewModel::loginDemo,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.label_demo_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.action_skip_setup))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SetupPreviewScreen(
    viewModel: MainViewModel,
    entries: List<SubstitutionEntry>
) {
    val isRoomFirst by viewModel.isRoomFirst.collectAsState()
    val autoFetchEnabled by viewModel.autoFetchEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val sortByPeriod by viewModel.sortByPeriod.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val autoUpdateCheck by viewModel.autoUpdateCheck.collectAsState()
    val entry = remember(entries) { entries.firstOrNull() } ?: sampleSetupEntry
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = dpv(24.dp, 40.dp), vertical = dpv(40.dp, 48.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.title_setup_customize),
                style = MaterialTheme.typography.headlineLargeEmphasized,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.desc_setup_customize_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.label_setup_preferences),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            SettingCard {
                SettingsRow(
                    title = stringResource(R.string.action_swap_data),
                    description = if (isRoomFirst) stringResource(R.string.desc_swap_default) else stringResource(R.string.desc_swap_active),
                    icon = Icons.Default.SwapHoriz,
                    isActive = !isRoomFirst,
                    trailing = { ExpressiveSwitch(checked = !isRoomFirst, onCheckedChange = { viewModel.toggleColumnOrder() }) }
                )
                SetupDivider()
                SettingsRow(
                    title = stringResource(R.string.label_auto_fetch),
                    description = stringResource(R.string.desc_auto_fetch),
                    icon = Icons.Default.Sync,
                    isActive = autoFetchEnabled,
                    trailing = { ExpressiveSwitch(checked = autoFetchEnabled, onCheckedChange = { viewModel.toggleAutoFetch() }) }
                )
                SetupDivider()
                SettingsRow(
                    title = stringResource(R.string.label_notifications),
                    description = stringResource(R.string.desc_notifications),
                    icon = Icons.Default.Notifications,
                    isActive = notificationsEnabled,
                    trailing = { ExpressiveSwitch(checked = notificationsEnabled, onCheckedChange = { viewModel.toggleNotifications() }) }
                )
                SetupDivider()
                SettingsRow(
                    title = stringResource(R.string.label_sort_period),
                    description = stringResource(R.string.desc_sort_period),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    isActive = sortByPeriod,
                    trailing = { ExpressiveSwitch(checked = sortByPeriod, onCheckedChange = { viewModel.toggleSortByPeriod() }) }
                )
                SetupDivider()
                SettingsRow(
                    title = stringResource(R.string.label_dynamic_color),
                    description = stringResource(R.string.desc_dynamic_color),
                    icon = Icons.Default.Palette,
                    isActive = dynamicColor,
                    trailing = { ExpressiveSwitch(checked = dynamicColor, onCheckedChange = { viewModel.toggleDynamicColor() }) }
                )
                SetupDivider()
                SettingsRow(
                    title = stringResource(R.string.label_auto_update_check),
                    description = stringResource(R.string.desc_auto_update_check),
                    icon = Icons.Default.Update,
                    isActive = autoUpdateCheck,
                    trailing = { ExpressiveSwitch(checked = autoUpdateCheck, onCheckedChange = { viewModel.setAutoUpdateCheck(!autoUpdateCheck) }) }
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.label_setup_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.desc_setup_preview_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.day,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (entries.isEmpty()) {
                            Text(
                                stringResource(R.string.desc_preview_placeholder),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                    SubstitutionTableRowContent(entry = entry, isRoomFirst = isRoomFirst)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))

                    val roomDisplay = if (isRoomFirst) entry.room else entry.art
                    val typeDisplay = if (isRoomFirst) entry.art else entry.room
                    SetupField(stringResource(R.string.label_day), entry.day)
                    SetupField(stringResource(R.string.label_period_short), entry.lesson)
                    SetupField(stringResource(R.string.label_subject_short), entry.subject)
                    SetupField(stringResource(R.string.label_field_class), entry.className)
                    SetupField(stringResource(R.string.label_room), roomDisplay)
                    SetupField(stringResource(R.string.label_type), typeDisplay)
                    SetupField(stringResource(R.string.label_cover), entry.vertrVon)
                    SetupField(stringResource(R.string.label_replaced), entry.nach)
                    SetupField(stringResource(R.string.label_note), entry.text)
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::finishSetup,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.action_finish_setup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SetupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

private val sampleSetupEntry = SubstitutionEntry(
    day = "Montag",
    className = "10a",
    lesson = "1 - 2",
    subject = "Mathematik",
    art = "Vertretung",
    room = "R101",
    vertrVon = "Herr Mustermann",
    nach = "Frau Beispiel",
    text = "Lehrer krank — Aufgaben werden im Klassenraum ausgeteilt",
    rawText = ""
)
