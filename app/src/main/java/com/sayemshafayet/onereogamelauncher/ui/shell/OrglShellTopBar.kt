package com.sayemshafayet.onereogamelauncher.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.sayemshafayet.onereogamelauncher.BuildConfig
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintBadge
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintOverlay
import com.sayemshafayet.onereogamelauncher.ui.input.rememberShowGamepadHints
import com.sayemshafayet.onereogamelauncher.ui.navigation.Routes
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameDetailViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeViewModel
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
            navController = navController,
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
                    }
                    IconButton(
                        onClick = viewModel::toggleLayout,
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) {
                        if (layout == GameListLayout.GRID) {
                            Icon(Icons.Default.ViewList, contentDescription = "List view")
                        } else {
                            Icon(Icons.Default.GridView, contentDescription = "Grid view")
                        }
                    }
                },
            )
        }
        currentRoute == Routes.SETUP_SYSTEM_EMULATOR -> SimpleTopAppBar("Emulator", onBack)
        currentRoute == Routes.SETUP_GAME -> backStackEntry?.let { entry ->
            val viewModel: GameDetailViewModel = hiltViewModel(entry)
            val game by viewModel.game.collectAsState()
            SimpleTopAppBar(game?.title ?: "Game", onBack)
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
        currentRoute == Routes.SETUP_SETTINGS_CREDITS -> SimpleTopAppBar("Credits", onBack)
        currentRoute == Routes.SETUP_ABOUT -> SimpleTopAppBar("About ORGL", onBack)
        currentRoute == Routes.SETUP_SCRAPE_WIZARD -> backStackEntry?.let { entry ->
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
    navController: NavHostController,
    onRequestSetupMode: () -> Unit,
    onRequestPlayMode: () -> Unit,
) {
    val showHints = rememberShowGamepadHints()
    TopAppBar(
        title = {
            if (isPlay) {
                Text("Play")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ORGL")
                    Text(
                        text = " ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
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
                if (!isPlay) {
                    Box {
                        FilledTonalIconButton(
                            onClick = { navController.navigate(Routes.SETUP_ABOUT) },
                            modifier = Modifier.focusProperties { canFocus = false },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "About ORGL",
                            )
                        }
                        if (showHints) {
                            GamepadHintOverlay(
                                label = "X",
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
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
    TopAppBar(
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
