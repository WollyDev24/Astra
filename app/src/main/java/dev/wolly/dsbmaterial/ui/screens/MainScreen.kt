@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import dev.wolly.dsbmaterial.api.UpdateChannel
import dev.wolly.dsbmaterial.ui.MainViewModel
import dev.wolly.dsbmaterial.ui.UiState
import dev.wolly.dsbmaterial.ui.UpdateState
import dev.wolly.dsbmaterial.ui.feedback
import dev.wolly.dsbmaterial.ui.components.*
import dev.wolly.dsbmaterial.ui.theme.fullRoundedShape
import dev.wolly.dsbmaterial.ui.theme.springDefaultEffects
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class Destination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSBApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isRoomFirst by viewModel.isRoomFirst.collectAsState()
    val sortByPeriod by viewModel.sortByPeriod.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val themeIndex by viewModel.themeIndex.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val archiveEntries by viewModel.archive.collectAsState()
    val navHidden by viewModel.navHidden.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedClasses by viewModel.selectedClasses.collectAsState()
    val autoFetchEnabled by viewModel.autoFetchEnabled.collectAsState()
    val autoFetchInterval by viewModel.autoFetchInterval.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val useCustomFont by viewModel.useCustomFont.collectAsState()
    val fontRond by viewModel.fontRond.collectAsState()
    val customServerUrl by viewModel.customServerUrl.collectAsState()
    val webServerEnabled by viewModel.webServerEnabled.collectAsState()
    val webServerUrls by viewModel.webServerUrls.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val updateChannel by viewModel.updateChannel.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val haptics = LocalHapticFeedback.current

    val destinations = listOf(
        Destination(stringResource(R.string.label_home), Icons.Filled.Home, Icons.Outlined.Home),
        Destination(stringResource(R.string.label_plans), Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        Destination(stringResource(R.string.label_archive), Icons.Filled.Archive, Icons.Outlined.Archive),
        Destination(stringResource(R.string.title_settings), Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val currentEntries = (uiState as? UiState.Success)?.entries.orEmpty()

    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val currentTab = pagerState.currentPage.coerceIn(destinations.indices)
    var showProfile by remember { mutableStateOf(false) }

    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        decayAnimationSpec = exponentialDecay(frictionMultiplier = 3f),
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)
    )

    LaunchedEffect(selectedTab) {
        showProfile = false
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(
                selectedTab,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)
            )
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }.collect { (page, scrolling) ->
            if (!scrolling && page != selectedTab) {
                viewModel.setTab(page)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect {
                if (pagerState.isScrollInProgress) {
                    haptics.feedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    val sheetState = rememberModalBottomSheetState()

    val scope = rememberCoroutineScope()
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    var isDismissing by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showUpdates by remember { mutableStateOf(false) }
    var calendarSelectedDay by remember { mutableStateOf<String?>(null) }
    var shareCardDay by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showSheet) {
        if (!showSheet) sheetState.hide()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val cardAlpha by animateFloatAsState(
        targetValue = when {
            isDismissing -> 1f
            selectedDay != null -> 0f
            else -> 1f
        },
        animationSpec = springDefaultEffects(),
        label = "cardAlpha"
    )

    val isTablet = isExpandedScreen()

    val overlayActive = showCalendar || showThemePicker || showAbout || showDebug || showUpdates || shareCardDay != null

    val showNavCondition = true

    BackHandler(enabled = showSheet || overlayActive || showProfile || uiState is UiState.SelectingClass || uiState is UiState.SetupPreview || selectedTab != 0) {
        if (showProfile) {
            showProfile = false
        } else if (showSheet) {
            if (isTablet) {
                scope.launch {
                    isDismissing = true
                    delay(250)
                    showSheet = false
                    selectedDay = null
                    isDismissing = false
                }
            } else {
                showSheet = false
                selectedDay = null
            }
        } else if (showCalendar) {
            showCalendar = false
        } else if (showThemePicker) {
            showThemePicker = false
        } else if (showAbout) {
            showAbout = false
        } else if (showUpdates) {
            showUpdates = false
        } else if (showDebug) {
            showDebug = false
        } else if (shareCardDay != null) {
            shareCardDay = null
        } else if (uiState is UiState.SelectingClass) {
            viewModel.cancelClassSelection()
        } else if (uiState is UiState.SetupPreview) {
            viewModel.finishSetup()
        } else {
            viewModel.setTab(0)
        }
    }

    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AnimatedVisibility(
            visible = isTablet && showNavCondition,
            enter = slideInHorizontally { -it } + fadeIn(tween(300)),
            exit = slideOutHorizontally(tween(0)) { -it } + fadeOut(tween(0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                NavigationRail(
                    modifier = Modifier.fillMaxSize().padding(top = 48.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    destinations.forEachIndexed { index, destination ->
                        NavigationRailItem(
                            selected = currentTab == index,
                            onClick = { viewModel.setTab(index) },
                            icon = {
                                Icon(
                                    if (currentTab == index) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text(destination.label, maxLines = 2, softWrap = true, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            alwaysShowLabel = true
                        )
                        if (index < destinations.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CollapsingTopBar(
                        title = destinations[currentTab].label,
                        actions = {
                            if (currentTab == 0) {
                                ProfileButton(username = username, onClick = { showProfile = true })
                            }
                            if (currentTab == 1 && (uiState is UiState.Success || uiState is UiState.Idle)) {
                                val refreshing = uiState is UiState.Loading
                                val refreshRotation by animateFloatAsState(
                                    targetValue = if (refreshing) 360f else 0f,
                                    animationSpec = if (refreshing) infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing)
                                    ) else tween(0),
                                    label = "refresh_rotation"
                                )
                                IconButton(onClick = { viewModel.fetchData() }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.action_refresh),
                                        modifier = Modifier.rotate(refreshRotation)
                                    )
                                }
                            } else if (currentTab == 2 && archiveEntries.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearArchive() }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.action_clear_archive))
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isTablet && showNavCondition) {
                        if (navHidden) {
                            FloatingNavigationBar(
                                destinations = destinations,
                                currentTab = currentTab,
                                onSelect = { viewModel.setTab(it) }
                            )
                        } else {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                                destinations.forEachIndexed { index, destination ->
                                    val selected = currentTab == index
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            haptics.feedback(HapticFeedbackType.LongPress)
                                            viewModel.setTab(index)
                                        },
                                        icon = {
                                            Icon(
                                                if (selected) destination.selectedIcon else destination.unselectedIcon,
                                                contentDescription = destination.label
                                            )
                                        },
                                        label = { Text(destination.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val contentPadding =
                    if (!isTablet && showNavCondition && navHidden) {
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = 0.dp
                        )
                    } else {
                        innerPadding
                    }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = dpv(640.dp, 840.dp))
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                flingBehavior = flingBehavior,
                                beyondViewportPageCount = 1
                            ) { page ->
                                when (page) {
                                    0 -> HomeScreen(
                                        entries = currentEntries,
                                        isRoomFirst = isRoomFirst,
                                        isRefreshing = isRefreshing,
                                        hasError = uiState is UiState.Error,
                                        lastUpdated = lastUpdated,
                                        isOffline = isOffline,
                                        onRefresh = { viewModel.fetchData() },
                                        onRetry = { viewModel.fetchData() }
                                    )
                                    1 -> {
                                        val currentUiState = uiState
                                    if (currentUiState is UiState.Success) {
                                        DayList(
                                            entries = currentUiState.entries,
                                            selectedDay = selectedDay,
                                            cardAlpha = cardAlpha,
                                            isRefreshing = isRefreshing,
                                            onRefresh = { viewModel.fetchData() },
                                            onDayClick = { day, bounds ->
                                                selectedDay = day
                                                cardRect = bounds
                                                showSheet = true
                                            }
                                        )
                                    } else if (currentUiState is UiState.Error) {
                                        ErrorScreen(currentUiState.message, onRetry = { viewModel.fetchData() })
                                    } else {
                                        Box(Modifier.fillMaxSize())
                                    }
                                }
                                2 -> ArchiveScreen(
                                    entries = archiveEntries,
                                    isRoomFirst = isRoomFirst,
                                    onRemoveGroup = { entries -> entries.forEach { viewModel.removeFromArchive(it) } },
                                    onOpenCalendar = { showCalendar = true }
                                )
                                3 -> SettingsScreen(
                                    isRoomFirst = isRoomFirst,
                                    sortByPeriod = sortByPeriod,
                                    dynamicColor = dynamicColor,
                                    navHidden = navHidden,
                                    hapticsEnabled = hapticsEnabled,
                                    selectedClasses = selectedClasses,
                                    autoFetchEnabled = autoFetchEnabled,
                                    autoFetchInterval = autoFetchInterval,
                                    notificationsEnabled = notificationsEnabled,
                                    onToggleOrder = viewModel::toggleColumnOrder,
                                    onToggleSort = viewModel::toggleSortByPeriod,
                                    onToggleDynamic = viewModel::toggleDynamicColor,
                                    onToggleNavHidden = viewModel::toggleNavHidden,
                                    onToggleHaptics = viewModel::toggleHaptics,
                                    onOpenThemePicker = { showThemePicker = true },
                                    useCustomFont = useCustomFont,
                                    fontRond = fontRond,
                                    onToggleCustomFont = viewModel::toggleCustomFont,
                                    onFontRondChange = { viewModel.setFontRond(it) },
                                    onToggleAutoFetch = viewModel::toggleAutoFetch,
                                    onSetAutoFetchInterval = { viewModel.setAutoFetchInterval(it) },
                                    onToggleNotifications = {
                                        if (!notificationsEnabled) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                viewModel.setNotificationsEnabled(true)
                                            }
                                        } else {
                                            viewModel.setNotificationsEnabled(false)
                                        }
                                    },
                                    onChangeClass = viewModel::changeClass,
                                    onLogout = viewModel::logout,
                                    customServerUrl = customServerUrl,
                                    onSetCustomServerUrl = viewModel::setCustomServerUrl,
                                    webServerEnabled = webServerEnabled,
                                    webServerUrls = webServerUrls,
                                    onToggleWebServer = viewModel::toggleWebServer,
                                    updateState = updateState,
                                    onOpenUpdates = { showUpdates = true },
                                    onAbout = { showAbout = true },
                                    onAddClass = viewModel::addSelectedClass,
                                    onRemoveClass = viewModel::removeSelectedClass
                                )
                                }
                            }
                            if (showProfile && showNavCondition) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showProfile = false }
                                )
                            }
                            ProfileOverlay(
                                showProfile = showProfile && showNavCondition,
                                username = username,
                                password = password,
                                onDismiss = { showProfile = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 16.dp)
                            )
                        }
                    }
                }
            }

            if (showSheet && selectedDay != null) {
                val currentState = uiState
                if (currentState is UiState.Success) {
                    val dayEntries = currentState.entries.filter { it.day == selectedDay }
                    if (isTablet) {
                        TabletSubstitutionPopup(
                            selectedDay = selectedDay!!,
                            entries = dayEntries,
                            isRoomFirst = isRoomFirst,
                            cardRect = cardRect,
                            onDismissStart = { isDismissing = true },
                            onDismiss = { showSheet = false; selectedDay = null; isDismissing = false },
                            onShare = { day -> shareCardDay = day }
                        )
                    } else {
                        ModalBottomSheet(
                            onDismissRequest = { showSheet = false },
                            sheetState = sheetState,
                            shape = MaterialTheme.shapes.extraLarge,
                            dragHandle = {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                                                    sheetState.expand()
                                                } else {
                                                    sheetState.partialExpand()
                                                }
                                            }
                                        }
                                        .padding(top = 16.dp, bottom = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BottomSheetDefaults.DragHandle()
                                }
                            }
                        ) {
                            SubstitutionViewer(selectedDay!!, dayEntries, isRoomFirst, true, onShare = { day ->
                                showSheet = false
                                selectedDay = null
                                shareCardDay = day
                            })
                        }
                    }
                }
            }

            OverlayContent(
                showThemePicker = showThemePicker,
                showAbout = showAbout,
                showDebug = showDebug,
                showCalendar = showCalendar,
                uiState = uiState,
                themeIndex = themeIndex,
                dynamicColor = dynamicColor,
                allArchiveEntries = archiveEntries,
                isRoomFirst = isRoomFirst,
                calendarEntries = remember(archiveEntries) {
                    val dateRegex = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
                    archiveEntries.groupBy { it.day }.map { (day, entries) -> day to entries.size }.sortedBy { day ->
                        val match = dateRegex.find(day.first)
                        if (match != null) {
                            val (d, m, y) = match.destructured
                            y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
                        } else Long.MAX_VALUE
                    }
                },
                calendarSelectedDay = calendarSelectedDay,
                onSelectTheme = { viewModel.setThemeIndex(it) },
                onCloseThemePicker = { showThemePicker = false },
                onCloseAbout = { showAbout = false },
                onCloseUpdates = { showUpdates = false },
                onOpenDebug = { showAbout = false; showDebug = true },
                onCloseDebug = { showDebug = false },
                onCalendarDayClick = { calendarSelectedDay = if (calendarSelectedDay == it) null else it },
                onCloseCalendar = { showCalendar = false },
                shareCardDay = shareCardDay,
                onOpenShareCard = { day -> shareCardDay = day },
                onCloseShareCard = { shareCardDay = null },
                onSelectClass = { u, p, cls -> viewModel.selectClass(u, p, cls) },
                onSelectAllClasses = { u, p -> viewModel.selectAllClasses(u, p) },
                onCancelClassSelection = { viewModel.cancelClassSelection() },
                onLogin = viewModel::login,
                onLoginDemo = viewModel::loginDemo,
                customServerUrl = customServerUrl,
                onSetCustomServerUrl = viewModel::setCustomServerUrl,
                onSkipSetup = viewModel::skipSetup,
                onFinishSetup = viewModel::finishSetup,
                showUpdates = showUpdates,
                updateState = updateState,
                updateChannel = updateChannel,
                onSelectChannel = viewModel::setUpdateChannel,
                onInstall = { viewModel.installUpdate() },
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun FloatingNavigationBar(
    destinations: List<Destination>,
    currentTab: Int,
    onSelect: (Int) -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var itemRects by remember { mutableStateOf<Map<Int, IntRect>>(emptyMap()) }
    var dragX by remember { mutableStateOf<Float?>(null) }
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var lastDragHapticIndex by remember { mutableStateOf<Int?>(null) }

    fun nearestIndex(x: Float): Int? =
        itemRects.minByOrNull { (_, r) -> abs((r.left + r.width / 2f) - x) }?.key

    LaunchedEffect(currentTab) {
        if (dragIndex != null && currentTab == dragIndex) {
            dragIndex = null
        }
    }

    val highlightIndex = dragIndex ?: currentTab

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .shadow(8.dp, fullRoundedShape()),
            shape = fullRoundedShape(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                val currentRect = itemRects[highlightIndex]
                val minLeftPx = itemRects.values.minOfOrNull { it.left }?.toFloat() ?: 0f
                val maxRightPx = itemRects.values.maxOfOrNull { it.right }?.toFloat() ?: 0f
                val pillSpring: SpringSpec<Float> = springDefaultSpatial()
                val pillWidthPx by animateFloatAsState(
                    targetValue = (currentRect?.width ?: 0).toFloat(),
                    animationSpec = pillSpring,
                    label = "pill_width"
                )
                val pillHeightPx by animateFloatAsState(
                    targetValue = (currentRect?.height ?: 0).toFloat(),
                    animationSpec = pillSpring,
                    label = "pill_height"
                )
                val pillLeftPx by animateFloatAsState(
                    targetValue = dragX?.let {
                        (it - pillWidthPx / 2f).coerceIn(minLeftPx, (maxRightPx - pillWidthPx).coerceAtLeast(minLeftPx))
                    } ?: (currentRect?.left ?: 0).toFloat(),
                    animationSpec = pillSpring,
                    label = "pill_left"
                )
                val pillTopPx by animateFloatAsState(
                    targetValue = (currentRect?.top ?: 0).toFloat(),
                    animationSpec = pillSpring,
                    label = "pill_top"
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pillLeftPx.roundToInt(), pillTopPx.roundToInt()) }
                        .size(with(density) { pillWidthPx.toDp() }, with(density) { pillHeightPx.toDp() })
                        .clip(fullRoundedShape())
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    dragX = offset.x
                                    val index = nearestIndex(offset.x)
                                    dragIndex = index
                                    lastDragHapticIndex = index
                                },
                                onHorizontalDrag = { change, _ ->
                                    dragX = change.position.x
                                    val index = nearestIndex(change.position.x)
                                    if (index != lastDragHapticIndex) {
                                        lastDragHapticIndex = index
                                        haptics.feedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    dragIndex = index
                                },
                                onDragEnd = {
                                    dragX?.let { x ->
                                        val index = nearestIndex(x)
                                        dragIndex = index
                                        index?.let(onSelect)
                                    }
                                    dragX = null
                                    lastDragHapticIndex = null
                                },
                                onDragCancel = {
                                    dragX = null
                                    dragIndex = null
                                    lastDragHapticIndex = null
                                }
                            )
                        }
                ) {
                    destinations.forEachIndexed { index, destination ->
                        val selected = highlightIndex == index
                        val iconTint by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = springDefaultEffects(),
                            label = "floating_nav_icon_tint"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(fullRoundedShape())
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptics.feedback(HapticFeedbackType.LongPress)
                                    onSelect(index)
                                }
                                .onGloballyPositioned {
                                    val bounds = it.boundsInParent()
                                    itemRects = itemRects + (index to IntRect(
                                        bounds.left.toInt(),
                                        bounds.top.toInt(),
                                        bounds.right.toInt(),
                                        bounds.bottom.toInt()
                                    ))
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileOverlay(
    showProfile: Boolean,
    username: String?,
    password: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showProfile,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(250)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150)),
        modifier = modifier
    ) {
        ProfilePopover(
            username = username,
            password = password,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun OverlayContent(
    showThemePicker: Boolean,
    showAbout: Boolean,
    showDebug: Boolean,
    showCalendar: Boolean,
    uiState: UiState,
    themeIndex: Int,
    dynamicColor: Boolean,
    calendarEntries: List<Pair<String, Int>>,
    allArchiveEntries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    calendarSelectedDay: String?,
    shareCardDay: String? = null,
    showUpdates: Boolean = false,
    updateState: UpdateState = UpdateState(),
    updateChannel: UpdateChannel = UpdateChannel.STABLE,
    onSelectChannel: (UpdateChannel) -> Unit = {},
    onInstall: () -> Unit = {},
    onOpenShareCard: (String) -> Unit = {},
    onCloseShareCard: () -> Unit = {},
    onSelectTheme: (Int) -> Unit,
    onCloseThemePicker: () -> Unit,
    onCloseAbout: () -> Unit,
    onCloseUpdates: () -> Unit = {},
    onOpenDebug: () -> Unit,
    onCloseDebug: () -> Unit,
    onCalendarDayClick: (String) -> Unit,
    onCloseCalendar: () -> Unit,
    onSelectClass: (String, String, String) -> Unit,
    onSelectAllClasses: (String, String) -> Unit,
    onCancelClassSelection: () -> Unit = {},
    onLogin: (String, String) -> Unit,
    onLoginDemo: () -> Unit,
    customServerUrl: String? = null,
    onSetCustomServerUrl: (String) -> Unit = {},
    onSkipSetup: () -> Unit = {},
    onFinishSetup: () -> Unit = {},
    viewModel: MainViewModel
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = showThemePicker || showAbout || showDebug || showCalendar || showUpdates || shareCardDay != null || uiState is UiState.NeedsLogin || uiState is UiState.NeedsSetup || uiState is UiState.Loading || uiState is UiState.SelectingClass || uiState is UiState.SetupPreview,
        enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
        exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.92f, animationSpec = tween(250))
    ) {
        when {
            shareCardDay != null -> PredictiveBackHost(onBack = onCloseShareCard) {
                ShareCardScreen(
                    day = shareCardDay!!,
                    entries = (uiState as? UiState.Success)?.entries?.filter { it.day == shareCardDay }.orEmpty(),
                    isRoomFirst = isRoomFirst,
                    themeIndex = themeIndex,
                    onBack = onCloseShareCard
                )
            }
            showCalendar -> PredictiveBackHost(onBack = onCloseCalendar) {
                CalendarViewScreen(
                    archiveDates = calendarEntries,
                    allArchiveEntries = allArchiveEntries,
                    isRoomFirst = isRoomFirst,
                    selectedDay = calendarSelectedDay,
                    onDayClick = onCalendarDayClick,
                    onBack = onCloseCalendar,
                    onShareDay = onOpenShareCard
                )
            }
            showUpdates -> PredictiveBackHost(onBack = onCloseUpdates) {
                UpdateScreen(
                    updateState = updateState,
                    updateChannel = updateChannel,
                    onSelectChannel = onSelectChannel,
                    onInstall = onInstall,
                    onBack = onCloseUpdates,
                    viewModel = viewModel
                )
            }
            showThemePicker -> PredictiveBackHost(onBack = onCloseThemePicker) {
                ThemePickerScreen(
                    currentIndex = themeIndex,
                    dynamicColor = dynamicColor,
                    onSelect = onSelectTheme,
                    onBack = onCloseThemePicker
                )
            }
            showAbout -> PredictiveBackHost(onBack = onCloseAbout) {
                AboutScreen(onBack = onCloseAbout, onDebugTap = onOpenDebug)
            }
            showDebug -> PredictiveBackHost(onBack = onCloseDebug) {
                DebugModeScreen(onBack = onCloseDebug)
            }
            uiState is UiState.Loading -> LoadingScreen()
            uiState is UiState.SelectingClass -> {
                val s = uiState
                PredictiveBackHost(onBack = onCancelClassSelection) {
                    ClassSelectionScreen(
                        classes = s.classes,
                        onClassSelected = { cls -> onSelectClass(s.u, s.p, cls) },
                        onShowAll = { onSelectAllClasses(s.u, s.p) },
                        onBack = onCancelClassSelection
                    )
                }
            }
            uiState is UiState.NeedsLogin -> LoginScreen(onLogin = onLogin, onLoginDemo = onLoginDemo, customServerUrl = customServerUrl, onSetCustomServerUrl = onSetCustomServerUrl)
            uiState is UiState.NeedsSetup -> SetupScreen(
                viewModel = viewModel,
                onSkip = onSkipSetup,
                customServerUrl = customServerUrl
            )
            uiState is UiState.SetupPreview -> SetupPreviewScreen(
                viewModel = viewModel,
                entries = uiState.entries
            )
        }
    }
}

@Composable
fun CalendarViewScreen(
    archiveDates: List<Pair<String, Int>>,
    allArchiveEntries: List<SubstitutionEntry>,
    isRoomFirst: Boolean,
    selectedDay: String?,
    onDayClick: (String) -> Unit,
    onBack: () -> Unit,
    onShareDay: (String) -> Unit = {}
) {
    BackHandler(enabled = selectedDay != null) {
        onDayClick(selectedDay!!)
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedDay != null) {
                        onDayClick(selectedDay)
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = stringResource(R.string.label_calendar),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (selectedDay != null) {
                val dayEntries = allArchiveEntries.filter { it.day == selectedDay }
                SubstitutionViewer(selectedDay, dayEntries, isRoomFirst, true, onShare = onShareDay)
            } else {
                CalendarView(
                    archiveDates = archiveDates,
                    selectedDay = selectedDay,
                    isRoomFirst = isRoomFirst,
                    onDayClick = onDayClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
