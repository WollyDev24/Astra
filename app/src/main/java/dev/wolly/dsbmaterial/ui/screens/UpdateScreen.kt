@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.BuildConfig
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.api.GitCommit
import dev.wolly.dsbmaterial.api.UpdateChannel
import dev.wolly.dsbmaterial.ui.CommitStatus
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.UpdateCheckStatus
import dev.wolly.dsbmaterial.ui.UpdateState
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import java.util.TimeZone

private val commitDateParser: SimpleDateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

private val commitDateFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

private fun formatCommitDate(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        commitDateParser.parse(iso)?.let { commitDateFormatter.format(it) }
    }.getOrNull() ?: iso
}

@Composable
fun UpdateScreen(
    updateState: UpdateState,
    updateChannel: UpdateChannel,
    onSelectChannel: (UpdateChannel) -> Unit,
    onInstallDev: () -> Unit,
    onDownloadUpdate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    LaunchedEffect(Unit) { viewModel.loadUpdatePage() }
    var changesExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = stringResource(R.string.label_updates),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "channel") {
                    FlyInFromFirstCard(index = 0) {
                        UpdateChannelCard(
                            updateChannel = updateChannel,
                            onSelectChannel = onSelectChannel
                        )
                    }
                }

                item(key = "status") {
                    FlyInFromFirstCard(index = 1) {
                        UpdateStatusCard(
                            updateState = updateState,
                            onDownloadUpdate = onDownloadUpdate,
                            onInstallDev = onInstallDev,
                            onCheckUpdates = { viewModel.checkForUpdates() }
                        )
                    }
                }

                item(key = "changes_header") {
                    FlyInFromFirstCard(index = 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { changesExpanded = !changesExpanded }
                                .padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.label_recent_changes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            val chevronRotation by animateFloatAsState(
                                targetValue = if (changesExpanded) 90f else 0f,
                                label = "changes_chevron"
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (changesExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp).rotate(chevronRotation)
                            )
                        }
                    }
                }

                when (updateState.commitStatus) {
                    CommitStatus.Loading -> item(key = "changes_loading") {
                        FlyInFromFirstCard(index = 3) {
                            MorphingUpdateLoading(stringResource(R.string.msg_loading_commits))
                        }
                    }
                    CommitStatus.Error -> item(key = "changes_error") {
                        FlyInFromFirstCard(index = 3) {
                            CommitErrorCard(onRetry = viewModel::refreshCommits)
                        }
                    }
                    CommitStatus.Loaded ->
                        if (updateState.commits.isEmpty()) {
                            item(key = "changes_empty") {
                                FlyInFromFirstCard(index = 3) {
                                    EmptyCommitsCard()
                                }
                            }
                        } else {
                            val visible = if (changesExpanded) updateState.commits else updateState.commits.take(1)
                            itemsIndexed(visible) { index, commit ->
                                FlyInFromFirstCard(index = 3 + index, animateImmediately = changesExpanded) {
                                    CommitRow(commit)
                                }
                            }
                        }
                    CommitStatus.Idle -> item(key = "changes_idle") {
                        FlyInFromFirstCard(index = 3) {
                            MorphingUpdateLoading(stringResource(R.string.msg_loading_commits))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlyInFromFirstCard(
    index: Int,
    modifier: Modifier = Modifier,
    animateImmediately: Boolean = false,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(if (animateImmediately) 0L else index * 80L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(400)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(400)) +
            slideInVertically(tween(400)) { -it },
        content = { content() }
    )
}

@Composable
private fun UpdateChannelCard(
    updateChannel: UpdateChannel,
    onSelectChannel: (UpdateChannel) -> Unit
) {
    SettingCard {
        SettingsRow(
            title = stringResource(R.string.label_update_channel),
            description = stringResource(R.string.desc_update_channel),
            icon = Icons.Filled.Tune,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        val channels = UpdateChannel.entries
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            channels.forEachIndexed { index, channel ->
                SegmentedButton(
                    selected = updateChannel == channel,
                    onClick = { onSelectChannel(channel) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = channels.size)
                ) {
                    Text(
                        text = stringResource(
                            when (channel) {
                                UpdateChannel.STABLE -> R.string.channel_stable
                                UpdateChannel.BETA -> R.string.channel_beta
                                UpdateChannel.DEV -> R.string.channel_dev
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(
    updateState: UpdateState,
    onDownloadUpdate: (String) -> Unit,
    onInstallDev: () -> Unit,
    onCheckUpdates: () -> Unit
) {
    val status = updateState.status
    val isDev = updateState.channel == UpdateChannel.DEV

    if (status == UpdateCheckStatus.Checking) {
        SettingCard {
            MorphingUpdateLoading(stringResource(R.string.msg_checking_updates))
        }
        return
    }

    SettingCard {
        SettingsRow(
            title = when {
                isDev && status == UpdateCheckStatus.Available -> stringResource(R.string.msg_dev_available)
                status == UpdateCheckStatus.Available -> stringResource(R.string.msg_update_available)
                status == UpdateCheckStatus.UpToDate -> stringResource(R.string.msg_up_to_date)
                status == UpdateCheckStatus.Error -> stringResource(R.string.msg_update_error)
                else -> stringResource(R.string.label_updates)
            },
            description = when {
                isDev && status == UpdateCheckStatus.Available ->
                    stringResource(R.string.desc_dev_available, formatCommitDate(updateState.update?.publishedAt ?: ""))
                status == UpdateCheckStatus.Available -> stringResource(R.string.desc_update_available, updateState.update?.version ?: "")
                status == UpdateCheckStatus.UpToDate -> stringResource(R.string.desc_up_to_date)
                status == UpdateCheckStatus.Error -> stringResource(R.string.desc_update_error)
                else -> stringResource(R.string.desc_check_updates)
            },
            icon = when (status) {
                UpdateCheckStatus.Available -> Icons.Default.SystemUpdate
                UpdateCheckStatus.UpToDate -> Icons.Default.CheckCircle
                UpdateCheckStatus.Error -> Icons.Default.ErrorOutline
                else -> Icons.Default.CloudDownload
            },
            iconContainerColor = when (status) {
                UpdateCheckStatus.Available -> MaterialTheme.colorScheme.tertiaryContainer
                UpdateCheckStatus.UpToDate -> MaterialTheme.colorScheme.secondaryContainer
                UpdateCheckStatus.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            isActive = status == UpdateCheckStatus.Available
        )
        Spacer(Modifier.height(4.dp))
        if (updateState.installing) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.msg_downloading_build),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (status) {
                    UpdateCheckStatus.Available -> {
                        Button(
                            onClick = {
                                if (isDev) {
                                    onInstallDev()
                                } else {
                                    updateState.update?.downloadUrl?.let(onDownloadUpdate)
                                    Unit
                                }
                            },
                            shape = fullRoundedShape(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (isDev) R.string.action_install else R.string.action_download))
                        }
                        TextButton(
                            onClick = onCheckUpdates,
                            shape = fullRoundedShape(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_check_updates), maxLines = 1, softWrap = false)
                        }
                    }
                    else -> {
                        val outlined = status == UpdateCheckStatus.Error || status == UpdateCheckStatus.UpToDate
                        val buttonModifier = Modifier.fillMaxWidth()
                        if (outlined) {
                            OutlinedButton(
                                onClick = onCheckUpdates,
                                shape = fullRoundedShape(),
                                modifier = buttonModifier
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_check_updates), maxLines = 1, softWrap = false)
                            }
                        } else {
                            TextButton(
                                onClick = onCheckUpdates,
                                shape = fullRoundedShape(),
                                modifier = buttonModifier
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_check_updates), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_current_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (status == UpdateCheckStatus.UpToDate && !isDev) {
                Text(
                    text = "v${updateState.update?.version ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MorphingUpdateLoading(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MorphingIconBackground(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(20.dp))
        LoadingIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommitRow(commit: GitCommit) {
    val initials = remember(commit.author) {
        commit.author.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "?" }
    }
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(fullRoundedShape())
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = commit.author,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (commit.author.isNotBlank() && formatCommitDate(commit.date).isNotBlank()) {
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCommitDate(commit.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = commit.sha,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CommitErrorCard(onRetry: () -> Unit) {
    SettingCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.msg_commits_error),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.desc_commits_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry, shape = fullRoundedShape()) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun EmptyCommitsCard() {
    SettingCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.msg_no_commits),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.desc_recent_changes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
