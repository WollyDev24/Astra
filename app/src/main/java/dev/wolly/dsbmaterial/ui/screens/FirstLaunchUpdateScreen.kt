@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.ui.UpdateCheckStatus
import dev.wolly.dsbmaterial.ui.UpdateState
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape

@Composable
fun FirstLaunchUpdateScreen(
    updateState: UpdateState,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    val status = updateState.status
    val containerColor = when (status) {
        UpdateCheckStatus.Available -> MaterialTheme.colorScheme.tertiaryContainer
        UpdateCheckStatus.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (status) {
        UpdateCheckStatus.Available -> MaterialTheme.colorScheme.onTertiaryContainer
        UpdateCheckStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MorphingIconBackground(
                color = containerColor,
                modifier = Modifier.size(140.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        UpdateCheckStatus.Available -> Icons.Default.CloudDownload
                        UpdateCheckStatus.Error -> Icons.Default.ErrorOutline
                        else -> Icons.Default.SystemUpdate
                    },
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = contentColor
                )
            }
            Spacer(Modifier.height(32.dp))

            when (status) {
                UpdateCheckStatus.Checking, UpdateCheckStatus.Idle -> {
                    Text(
                        text = stringResource(R.string.msg_checking_updates),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    LoadingIndicator()
                }

                UpdateCheckStatus.Available -> {
                    Text(
                        text = stringResource(R.string.msg_update_available),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.desc_update_available, updateState.update?.version ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(40.dp))

                    Button(
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = fullRoundedShape()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_install), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = fullRoundedShape()
                    ) {
                        Text(stringResource(R.string.action_skip), style = MaterialTheme.typography.titleMedium)
                    }
                }

                UpdateCheckStatus.UpToDate -> {
                    Text(
                        text = stringResource(R.string.msg_up_to_date),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.desc_up_to_date),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = fullRoundedShape()
                    ) {
                        Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                UpdateCheckStatus.Error -> {
                    Text(
                        text = stringResource(R.string.msg_update_error),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.desc_update_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(40.dp))
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = fullRoundedShape()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = fullRoundedShape()
                    ) {
                        Text(stringResource(R.string.action_skip), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}