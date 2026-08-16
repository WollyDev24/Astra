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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import dev.wolly.dsbmaterial.BuildConfig
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.UiState
import dev.wolly.dsbmaterial.ui.components.*
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import androidx.graphics.shapes.Morph
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator()
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.msg_loading),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400))
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

            AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400, delayMillis = 120)) + slideInVertically { it / 4 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.msg_error), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(32.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400, delayMillis = 240)) + slideInVertically { it / 4 }
        ) {
            Button(onClick = onRetry, shape = fullRoundedShape(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onLoginDemo: () -> Unit, customServerUrl: String? = null, onSetCustomServerUrl: (String) -> Unit = {}) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showCustomServer by remember { mutableStateOf(false) }
    var customUrl by remember(customServerUrl) { mutableStateOf(customServerUrl ?: "") }
    val scrollState = rememberScrollState()

    // Detect if keyboard is open to scale the icon
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val iconSize by animateDpAsState(
        targetValue = if (isKeyboardOpen) 60.dp else 100.dp,
        animationSpec = springDefaultSpatial(),
        label = "icon_size"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = dpv(32.dp, 48.dp), vertical = dpv(48.dp, 64.dp)),
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
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.title_login), style = MaterialTheme.typography.headlineLargeEmphasized, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(40.dp))
        
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
                    onSetCustomServerUrl(it)
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
                if (customUrl.isNotBlank()) onSetCustomServerUrl(customUrl)
                onLogin(username, password)
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = CircleShape,
            enabled = username.isNotBlank() && password.isNotBlank()
        ) {
            Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onLoginDemo,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = CircleShape
        ) {
            Text(stringResource(R.string.label_demo_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        // Extra space at bottom to ensure scrollability when keyboard is up
        Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun MorphingIconBackground(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shapes = passwordHintShapes
    val morphs = remember(shapes) {
        shapes.mapIndexed { i, shape ->
            Morph(shape, shapes[(i + 1) % shapes.size])
        }
    }
    val morphProgress = remember { Animatable(0f) }
    var morphIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(shapes) {
        val spec = spring(dampingRatio = 0.6f, stiffness = 200f, visibilityThreshold = 0.1f)
        while (true) {
            val deferred = launch {
                morphProgress.animateTo(1f, spec)
                morphIndex = (morphIndex + 1) % morphs.size
                morphProgress.snapTo(0f)
            }
            delay(650L)
            deferred.join()
        }
    }
    Box(
        modifier = modifier
            .clip(PasswordMorphShape(morphs[morphIndex], morphProgress.value))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
internal fun PasswordShapeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(fullRoundedShape())
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = borderColor,
                shape = fullRoundedShape()
            )
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.label_password),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            PasswordDisplays(
                password = value,
                revealed = false,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            interactionSource = interactionSource,
            decorationBox = { it() }
        )
    }
}

@Composable
fun ClassSelectionScreen(classes: List<String>, onClassSelected: (String) -> Unit, onShowAll: () -> Unit = {}, onBack: () -> Unit = {}) {
    var customClass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 24.dp, top = 56.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.title_select_class), style = MaterialTheme.typography.headlineLargeEmphasized, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.desc_select_class), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = customClass,
            onValueChange = { customClass = it },
            label = { Text(stringResource(R.string.label_manual_class)) },
            modifier = Modifier.fillMaxWidth(),
            shape = fullRoundedShape(),
            trailingIcon = {
                if (customClass.isNotEmpty()) {
                    IconButton(
                        onClick = { onClassSelected(customClass) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.action_submit))
                    }
                }
            }
        )
        
        Spacer(Modifier.height(32.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Surface(
                    onClick = onShowAll,
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.label_show_all_classes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(classes, key = { it }) { cls ->
                Surface(
                    onClick = { onClassSelected(cls) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(16.dp))
                        Text(cls, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


