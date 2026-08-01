package com.sayemshafayet.onereogamelauncher.ui.shell

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.sayemshafayet.onereogamelauncher.ui.util.orglDisplayVersionName
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.data.prefs.resolveLibrarySystemsLayout
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintBadge
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintOverlay
import com.sayemshafayet.onereogamelauncher.ui.components.mediaTypeLabel
import com.sayemshafayet.onereogamelauncher.ui.input.rememberShowGamepadHints
import com.sayemshafayet.onereogamelauncher.ui.navigation.Routes
import com.sayemshafayet.onereogamelauncher.ui.theme.GcBody
import com.sayemshafayet.onereogamelauncher.ui.theme.isOrglGcTheme
import com.sayemshafayet.onereogamelauncher.ui.theme.orglChromeAccentColor
import com.sayemshafayet.onereogamelauncher.ui.theme.usesOrglAtmosphereTheme
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameDetailViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryViewModel
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.MediaViewerViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeViewModel
import com.sayemshafayet.onereogamelauncher.ui.util.rememberPhysicalLandscapeAspectRatio
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeWizardStep
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SystemGamesViewModel

private val setupHubRoutes = setOf(
    Routes.SETUP_LIBRARY,
    Routes.SETUP_SETTINGS,
    Routes.SETUP_HISTORY,
)

private fun isHubRoute(route: String?): Boolean =
    route in setupHubRoutes || Routes.isPlayHubRoute(route)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrglShellTopBar(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry?,
    currentRoute: String?,
    isPlay: Boolean,
    canPopBack: Boolean,
    onRequestSetupMode: () -> Unit,
    onRequestPlayMode: () -> Unit,
) {
    val onBack: () -> Unit = { navController.popBackStack(); Unit }

    when {
        isHubRoute(currentRoute) -> HubTopAppBar(
            isPlay = isPlay,
            currentRoute = currentRoute,
            navController = navController,
            backStackEntry = backStackEntry,
            onRequestSetupMode = onRequestSetupMode,
            onRequestPlayMode = onRequestPlayMode,
        )
        currentRoute == Routes.SETUP_SYSTEM -> backStackEntry?.let { entry ->
            val viewModel: SystemGamesViewModel = hiltViewModel(entry)
            val title by viewModel.screenTitle.collectAsState()
            val layout by viewModel.layout.collectAsState()
            SimpleTopAppBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (!viewModel.isVirtualSystem) {
                        IconButton(
                            onClick = {
                                navController.navigate(Routes.setupSystemEmulator(viewModel.systemId))
                            },
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Emulator settings")
                        }
                        IconButton(
                            onClick = {
                                navController.navigate(Routes.setupScrapeWizard(viewModel.systemId))
                            },
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            Icon(Icons.Default.ImageSearch, contentDescription = "Scrape media")
                        }
                    }
                    IconButton(
                        onClick = viewModel::toggleLayout,
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        if (layout == GameListLayout.GRID) {
                            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "List view")
                        } else {
                            Icon(Icons.Default.GridView, contentDescription = "Grid view")
                        }
                    }
                },
            )
        }
        currentRoute == Routes.SETUP_LIBRARY_SEARCH -> SimpleTopAppBar("Search library", onBack)
        currentRoute == Routes.SETUP_SYSTEM_EMULATOR -> SimpleTopAppBar("Emulator", onBack)
        currentRoute == Routes.SETUP_GAME -> backStackEntry?.let { entry ->
            val viewModel: GameDetailViewModel = hiltViewModel(entry)
            val game by viewModel.game.collectAsState()
            SimpleTopAppBar(game?.title ?: "Game", onBack)
        }
        currentRoute == Routes.SETUP_GAME_MEDIA -> backStackEntry?.let { entry ->
            val viewModel: MediaViewerViewModel = hiltViewModel(entry)
            val media by viewModel.media.collectAsState()
            SimpleTopAppBar(
                title = media?.let { mediaTypeLabel(it.type) } ?: "Media",
                onBack = onBack,
            )
        }
        currentRoute == Routes.SETUP_GAME_RA -> SimpleTopAppBar("RetroAchievements", onBack)
        currentRoute == Routes.SETUP_SETTINGS_FOLDERS -> SimpleTopAppBar("Library folders", onBack)
        currentRoute == Routes.SETUP_LIBRARY_SCAN -> SimpleTopAppBar("Scanning library", onBack = null)
        currentRoute == Routes.SETUP_SETTINGS_ESDE -> SimpleTopAppBar("ES-DE", onBack)
        currentRoute == Routes.SETUP_SETTINGS_SCREENSCRAPER -> SimpleTopAppBar("ScreenScraper", onBack)
        currentRoute == Routes.SETUP_SETTINGS_RA -> SimpleTopAppBar("RetroAchievements", onBack)
        currentRoute == Routes.SETUP_SETTINGS_HLTB -> SimpleTopAppBar("HowLongToBeat", onBack)
        currentRoute == Routes.SETUP_SETTINGS_RETROARCH -> SimpleTopAppBar("RetroArch", onBack)
        currentRoute == Routes.SETUP_SETTINGS_PLAY_SLOTS -> SimpleTopAppBar("Now Playing Slots", onBack)
        currentRoute == Routes.SETUP_SETTINGS_DATABASE -> SimpleTopAppBar("Database", onBack)
        currentRoute == Routes.SETUP_SETTINGS_SYSTEM_INFO -> SimpleTopAppBar("System Info", onBack)
        currentRoute == Routes.SETUP_SETTINGS_CREDITS -> SimpleTopAppBar("Credits", onBack)
        currentRoute == Routes.SETUP_ABOUT -> SimpleTopAppBar("About ORGL", onBack)
        currentRoute?.startsWith("setup/legal/") == true -> SimpleTopAppBar(
            title = when {
                currentRoute.endsWith("/privacy") -> "Privacy Policy"
                currentRoute.endsWith("/terms") -> "Terms of Service"
                else -> "Legal"
            },
            onBack = onBack,
        )
        currentRoute?.startsWith("setup/scrape/wizard") == true -> backStackEntry?.let { entry ->
            val viewModel: ScrapeViewModel = hiltViewModel(entry)
            val wizard by viewModel.wizard.collectAsState()
            val session by viewModel.session.collectAsState()
            val title = when (wizard.step) {
                ScrapeWizardStep.SYSTEMS -> "Select systems"
                ScrapeWizardStep.OPTIONS -> "Scrape options"
                ScrapeWizardStep.PROGRESS -> "Scraping"
            }
            SimpleTopAppBar(
                title = title,
                onBack = {
                    when (wizard.step) {
                        ScrapeWizardStep.SYSTEMS -> onBack()
                        ScrapeWizardStep.OPTIONS -> viewModel.goToSystems()
                        ScrapeWizardStep.PROGRESS -> {
                            if (!session.running) onBack()
                        }
                    }
                },
                backEnabled = wizard.step != ScrapeWizardStep.PROGRESS || !session.running,
            )
        }
        currentRoute == Routes.SETUP_HISTORY -> SimpleTopAppBar(
            "History",
            onBack = if (canPopBack) onBack else null,
        )
        currentRoute == Routes.SETUP_HISTORY_RUN -> SimpleTopAppBar("Run card", onBack)
        currentRoute?.startsWith("play/complete/") == true -> SimpleTopAppBar("Run complete", onBack = null)
        currentRoute?.startsWith("play/commit/") == true -> SimpleTopAppBar("Confirm selection", onBack)
        else -> SimpleTopAppBar("", onBack = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubTopAppBar(
    isPlay: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    backStackEntry: NavBackStackEntry?,
    onRequestSetupMode: () -> Unit,
    onRequestPlayMode: () -> Unit,
) {
    val showHints = rememberShowGamepadHints()
    val showLibrarySearch = !isPlay && currentRoute == Routes.SETUP_LIBRARY
    val configuration = LocalConfiguration.current
    val landscapeAspectRatio = rememberPhysicalLandscapeAspectRatio()
    val isPortraitOrientation =
        configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val showLibrarySystemsLayoutToggle = showLibrarySearch && !isPortraitOrientation
    val versionStyle = MaterialTheme.typography.labelSmall.let { base ->
        base.copy(fontSize = (base.fontSize.value - 2f).sp)
    }
    val displayVersion = orglDisplayVersionName()
    val atmosphere = usesOrglAtmosphereTheme()
    val gc = isOrglGcTheme()
    val chromeAccent = orglChromeAccentColor()
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = when {
            gc -> GcBody
            atmosphere -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
            else -> Color.Unspecified
        },
        titleContentColor = chromeAccent,
        actionIconContentColor = chromeAccent,
        navigationIconContentColor = chromeAccent,
    )

    TopAppBar(
        colors = barColors,
        title = {
            if (isPlay) {
                Text("Play")
            } else {
                Column {
                    Text("ORGL")
                    Text(
                        text = displayVersion,
                        style = versionStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .focusProperties { canFocus = false },
            ) {
                if (showLibrarySystemsLayoutToggle && backStackEntry != null) {
                    val libraryViewModel: LibraryViewModel = hiltViewModel(backStackEntry)
                    val systemsLayoutPreference by libraryViewModel.systemsLayoutPreference.collectAsState()
                    val systemsLayout = resolveLibrarySystemsLayout(
                        preference = systemsLayoutPreference,
                        orientation = configuration.orientation,
                        landscapeAspectRatio = landscapeAspectRatio,
                    )
                    FilledTonalIconButton(
                        onClick = { libraryViewModel.toggleSystemsLayout(systemsLayout) },
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        if (systemsLayout == GameListLayout.GRID) {
                            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = "List view")
                        } else {
                            Icon(Icons.Default.GridView, contentDescription = "Grid view")
                        }
                    }
                }
                if (showLibrarySearch) {
                    Box {
                        FilledTonalIconButton(
                            onClick = { navController.navigate(Routes.SETUP_LIBRARY_SEARCH) },
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search library")
                        }
                        if (showHints) {
                            GamepadHintOverlay(
                                label = "X",
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    }
                }
                if (!isPlay) {
                    FilledTonalIconButton(
                        onClick = { navController.navigate(Routes.SETUP_ABOUT) },
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "About ORGL",
                        )
                    }
                }
                Box {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        SegmentedButton(
                            selected = !isPlay,
                            onClick = onRequestSetupMode,
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) { Text("Setup") }
                        SegmentedButton(
                            selected = isPlay,
                            onClick = onRequestPlayMode,
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) { Text("Play") }
                    }
                    if (showHints) {
                        GamepadHintBadge(
                            label = "Y",
                            compact = true,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-6).dp),
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    backEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val showHints = rememberShowGamepadHints()
    val atmosphere = usesOrglAtmosphereTheme()
    val gc = isOrglGcTheme()
    val chromeAccent = orglChromeAccentColor()
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = when {
            gc -> GcBody
            atmosphere -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
            else -> Color.Unspecified
        },
        titleContentColor = chromeAccent,
        actionIconContentColor = chromeAccent,
        navigationIconContentColor = chromeAccent,
    )
    TopAppBar(
        colors = barColors,
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                Box {
                    IconButton(
                        onClick = onBack,
                        enabled = backEnabled,
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    if (showHints && backEnabled) {
                        GamepadHintOverlay(
                            label = "B",
                            modifier = Modifier.align(Alignment.TopStart),
                            offsetX = 0.dp,
                            offsetY = (-2).dp,
                        )
                    }
                }
            }
        },
        actions = actions,
    )
}
