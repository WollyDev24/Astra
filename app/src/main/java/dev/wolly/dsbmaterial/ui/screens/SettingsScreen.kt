@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import dev.wolly.dsbmaterial.BuildConfig
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.ui.components.ExpressiveSwitch
import dev.wolly.dsbmaterial.ui.components.FontSlider
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import dev.wolly.dsbmaterial.ui.theme.springDefaultEffects
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import dev.wolly.dsbmaterial.ui.UpdateCheckStatus
import dev.wolly.dsbmaterial.ui.UpdateState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    isRoomFirst: Boolean,
    sortByPeriod: Boolean,
    dynamicColor: Boolean,
    navHidden: Boolean,
    selectedClasses: List<String> = emptyList(),
    autoFetchEnabled: Boolean = false,
    autoFetchInterval: Int = 30,
    notificationsEnabled: Boolean = true,
    onToggleOrder: () -> Unit,
    onToggleSort: () -> Unit,
    onToggleDynamic: () -> Unit,
    onToggleNavHidden: () -> Unit,
    onOpenThemePicker: () -> Unit,
    useCustomFont: Boolean = false,
    fontRond: Float = 0f,
    onToggleCustomFont: () -> Unit = {},
    onFontRondChange: (Float) -> Unit = {},
    onToggleAutoFetch: () -> Unit = {},
    onSetAutoFetchInterval: (Int) -> Unit = {},
    onToggleNotifications: () -> Unit = {},
    onChangeClass: () -> Unit,
    onLogout: () -> Unit,
    customServerUrl: String? = null,
    onSetCustomServerUrl: (String) -> Unit = {},
    webServerEnabled: Boolean = false,
    webServerUrls: List<String> = emptyList(),
    onToggleWebServer: () -> Unit = {},
    updateState: UpdateState = UpdateState(),
    onOpenUpdates: () -> Unit = {},
    onAbout: () -> Unit,
    onAddClass: (String) -> Unit = {},
    onRemoveClass: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var newClassName by remember { mutableStateOf("") }
    var showAddClassField by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSectionHeader(stringResource(R.string.label_preferences), modifier = Modifier.padding(top = 8.dp))
        }

        item {
            SettingCard {
                SettingsRow(
                    title = stringResource(R.string.action_swap_data),
                    description = if (isRoomFirst) stringResource(R.string.desc_swap_default) else stringResource(R.string.desc_swap_active),
                    icon = Icons.Default.SwapHoriz,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    isActive = !isRoomFirst,
                    trailing = { ExpressiveSwitch(checked = !isRoomFirst, onCheckedChange = { onToggleOrder() }) }
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.label_sort_period),
                    description = stringResource(R.string.desc_sort_period),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    isActive = sortByPeriod,
                    trailing = { ExpressiveSwitch(checked = sortByPeriod, onCheckedChange = { onToggleSort() }) }
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.label_dynamic_color),
                    description = stringResource(R.string.desc_dynamic_color),
                    icon = Icons.Default.Palette,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    isActive = dynamicColor,
                    trailing = { ExpressiveSwitch(checked = dynamicColor, onCheckedChange = { onToggleDynamic() }) }
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.label_floating_nav),
                    description = stringResource(R.string.desc_floating_nav),
                    icon = Icons.Default.Circle,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    isActive = navHidden,
                    trailing = { ExpressiveSwitch(checked = navHidden, onCheckedChange = { onToggleNavHidden() }) }
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.label_typography),
                    description = stringResource(R.string.desc_custom_font),
                    icon = Icons.Default.FormatSize,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    isActive = useCustomFont,
                    trailing = { ExpressiveSwitch(checked = useCustomFont, onCheckedChange = { onToggleCustomFont() }) }
                )
                if (useCustomFont) {
                    SettingsDivider()
                    var localRond by remember { mutableFloatStateOf(fontRond) }
                    LaunchedEffect(fontRond) { localRond = fontRond }
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        FontSlider(
                            label = stringResource(R.string.label_font_rond),
                            value = localRond,
                            valueRange = 0f..100f,
                            steps = 0,
                            displayValue = { v -> "${v.toInt()}" },
                            onValueChange = { localRond = it },
                            onValueChangeFinished = { onFontRondChange(localRond) }
                        )
                    }
                }
                if (!dynamicColor) {
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.label_theme_picker),
                        description = stringResource(R.string.desc_theme_picker),
                        icon = Icons.Default.ColorLens,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onOpenThemePicker
                    )
                }
            }
        }

        item {
            SettingsSectionHeader(stringResource(R.string.label_auto_fetch), modifier = Modifier.padding(top = 16.dp))
        }

        item {
            SettingCard {
                SettingsRow(
                    title = stringResource(R.string.label_auto_fetch),
                    description = stringResource(R.string.desc_auto_fetch),
                    icon = Icons.Default.Sync,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    isActive = autoFetchEnabled,
                    trailing = { ExpressiveSwitch(checked = autoFetchEnabled, onCheckedChange = { onToggleAutoFetch() }) }
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.label_notifications),
                    description = stringResource(R.string.desc_notifications),
                    icon = Icons.Default.Notifications,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    isActive = notificationsEnabled,
                    trailing = { ExpressiveSwitch(checked = notificationsEnabled, onCheckedChange = { onToggleNotifications() }) }
                )
                if (autoFetchEnabled) {
                    SettingsDivider()
                    var sliderValue by remember { mutableFloatStateOf(autoFetchInterval.toFloat()) }
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.label_fetch_interval), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.format_interval, sliderValue.toInt()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = { onSetAutoFetchInterval(sliderValue.toInt()) },
                            valueRange = 15f..120f,
                            steps = 6
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(stringResource(R.string.label_multi_class), modifier = Modifier.padding(top = 16.dp))
        }

        item {
            SettingCard {
                Text(
                    stringResource(R.string.label_multi_class_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (selectedClasses.isNotEmpty()) {
                    selectedClasses.forEach { cls ->
                        SettingsDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(cls, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveClass(cls) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove_class), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                SettingsDivider()
                if (showAddClassField) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newClassName,
                            onValueChange = { newClassName = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.label_class_hint)) },
                            singleLine = true,
                            shape = fullRoundedShape()
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newClassName.isNotBlank()) {
                                    onAddClass(newClassName)
                                    newClassName = ""
                                    showAddClassField = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.label_add_class), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showAddClassField = true },
                        shape = fullRoundedShape(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.label_add_class))
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(stringResource(R.string.label_account), modifier = Modifier.padding(top = 16.dp))
        }

        item {
            SettingCard {
                SettingsRow(
                    title = stringResource(R.string.action_switch_class),
                    description = stringResource(R.string.desc_switch_class),
                    icon = Icons.Default.School,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onChangeClass
                )
                SettingsDivider()
                SettingsRow(
                    title = stringResource(R.string.action_logout),
                    description = stringResource(R.string.desc_logout),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onLogout
                )
            }
        }

        item {
            SettingsSectionHeader(stringResource(R.string.label_server), modifier = Modifier.padding(top = 16.dp))
        }

        item {
            var showServerField by remember { mutableStateOf(false) }
            var serverUrl by remember(customServerUrl) { mutableStateOf(customServerUrl ?: "") }
            val serverTransition = remember { MutableTransitionState(false) }
            serverTransition.targetState = showServerField
            val arrowRotation by animateFloatAsState(
                targetValue = if (showServerField) 90f else 0f,
                animationSpec = springDefaultSpatial(),
                label = "server_arrow_rotation"
            )
            LaunchedEffect(showServerField) {
                if (showServerField) listState.animateScrollBy(Int.MAX_VALUE.toFloat())
            }
            LaunchedEffect(serverTransition.currentState) {
                if (serverTransition.currentState) listState.animateScrollBy(Int.MAX_VALUE.toFloat())
            }
            SettingCard {
                SettingsRow(
                    title = if (customServerUrl != null) "Custom Server" else stringResource(R.string.label_custom_server),
                    description = if (customServerUrl != null) customServerUrl else stringResource(R.string.desc_custom_server),
                    icon = Icons.Default.Dns,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = arrowRotation }
                        )
                    },
                    onClick = { showServerField = !showServerField }
                )
                AnimatedVisibility(visibleState = serverTransition, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column {
                        SettingsDivider()
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text(stringResource(R.string.label_custom_server)) },
                            placeholder = { Text("http://localhost:8080") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = fullRoundedShape(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onSetCustomServerUrl(serverUrl)
                                    showServerField = false
                                },
                                shape = fullRoundedShape()
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                            if (customServerUrl != null) {
                                OutlinedButton(
                                    onClick = {
                                        serverUrl = ""
                                        onSetCustomServerUrl("")
                                        showServerField = false
                                    },
                                    shape = fullRoundedShape()
                                ) {
                                    Text(stringResource(R.string.action_reset))
                                }
                            }
                        }
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(stringResource(R.string.label_webserver), modifier = Modifier.padding(top = 16.dp))
        }

        item {
            @Suppress("DEPRECATION")
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            var copied by remember { mutableStateOf(false) }
            SettingCard {
                SettingsRow(
                    title = stringResource(R.string.label_webserver),
                    description = if (webServerEnabled) stringResource(R.string.desc_webserver_running)
                                 else stringResource(R.string.desc_webserver),
                    icon = Icons.Default.Lan,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    trailing = { ExpressiveSwitch(checked = webServerEnabled, onCheckedChange = onToggleWebServer) },
                    onClick = onToggleWebServer
                )
                if (webServerEnabled) {
                    SettingsDivider()
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.desc_webserver_urls),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (webServerUrls.isEmpty()) {
                            Text(
                                text = stringResource(R.string.msg_webserver_no_url),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            webServerUrls.forEach { url ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                                        )
                                        IconButton(onClick = {
                                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(url))
                                            copied = true
                                        }) {
                                            Icon(
                                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = stringResource(R.string.action_copy_url),
                                                tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.desc_webserver_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionHeader(stringResource(R.string.label_updates), modifier = Modifier.padding(top = 16.dp))
        }

        item {
            val status = updateState.status
            SettingCard {
                SettingsRow(
                    title = when (status) {
                        UpdateCheckStatus.Available -> stringResource(R.string.msg_update_available)
                        UpdateCheckStatus.Checking -> stringResource(R.string.msg_checking_updates)
                        UpdateCheckStatus.UpToDate -> stringResource(R.string.msg_up_to_date)
                        UpdateCheckStatus.Error -> stringResource(R.string.msg_update_error)
                        UpdateCheckStatus.Idle -> stringResource(R.string.label_updates)
                    },
                    description = when (status) {
                        UpdateCheckStatus.Available -> stringResource(R.string.desc_update_available, updateState.update?.version ?: "")
                        UpdateCheckStatus.Checking -> stringResource(R.string.desc_checking_updates)
                        UpdateCheckStatus.UpToDate -> stringResource(R.string.desc_up_to_date)
                        UpdateCheckStatus.Error -> stringResource(R.string.desc_update_error)
                        UpdateCheckStatus.Idle -> stringResource(R.string.desc_check_updates)
                    },
                    icon = when (status) {
                        UpdateCheckStatus.Available -> Icons.Default.SystemUpdate
                        UpdateCheckStatus.Checking -> Icons.Default.Sync
                        UpdateCheckStatus.UpToDate -> Icons.Default.CheckCircle
                        UpdateCheckStatus.Error -> Icons.Default.ErrorOutline
                        UpdateCheckStatus.Idle -> Icons.Default.CloudDownload
                    },
                    iconContainerColor = when (status) {
                        UpdateCheckStatus.Available -> MaterialTheme.colorScheme.tertiaryContainer
                        UpdateCheckStatus.UpToDate -> MaterialTheme.colorScheme.secondaryContainer
                        UpdateCheckStatus.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    isActive = status == UpdateCheckStatus.Available,
                    trailing = {
                        if (status == UpdateCheckStatus.Checking) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        }
                    },
                    onClick = onOpenUpdates
                )
            }
        }

        item {
            SettingCard {
                SettingsRow(
                    title = stringResource(R.string.label_about),
                    description = stringResource(R.string.desc_about),
                    icon = Icons.Default.Info,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onAbout
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isActive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedIconBg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer
                      else iconContainerColor,
        animationSpec = springDefaultEffects(),
        label = "setting_icon_bg"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                      else iconTint,
        animationSpec = springDefaultEffects(),
        label = "setting_icon_tint"
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = springDefaultSpatial(),
        label = "setting_item_scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(animatedIconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = animatedIconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
