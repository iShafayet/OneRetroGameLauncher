package com.sayemshafayet.onereogamelauncher.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadBackHandler
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadKeys
import com.sayemshafayet.onereogamelauncher.ui.input.OrglBottomNavStrip
import com.sayemshafayet.onereogamelauncher.ui.input.cycleTabIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.domain.AppMode
import com.sayemshafayet.onereogamelauncher.ui.navigation.Routes
import com.sayemshafayet.onereogamelauncher.ui.play.CommitConfirmScreen
import com.sayemshafayet.onereogamelauncher.ui.play.FocusScreen
import com.sayemshafayet.onereogamelauncher.ui.play.JournalScreen
import com.sayemshafayet.onereogamelauncher.ui.play.PlayPickerScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.AboutScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.CreditsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.EsdeSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.GameRetroAchievementsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.GameDetailScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.HltbSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryFoldersScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryScanScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.RetroAchievementsSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.RetroArchSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScrapeScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScrapeWizardScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScreenScraperSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SystemEmulatorSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SystemGamesScreen
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.MainViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ModeShellViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings = settingsRepository.settings

    fun setMode(mode: AppMode) {
        viewModelScope.launch { settingsRepository.setAppMode(mode) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeShell(
    mainViewModel: MainViewModel = hiltViewModel(),
    shellViewModel: ModeShellViewModel = hiltViewModel(),
) {
    val settings by shellViewModel.settings.collectAsState(initial = AppSettings())
    val activeCommitment by mainViewModel.activeCommitment.collectAsState()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val isPlay = settings.appMode == AppMode.PLAY
    val setupTabs = setOf(
        Routes.SETUP_LIBRARY,
        Routes.SETUP_SETTINGS,
        Routes.SETUP_SCRAPE,
    )
    val immersiveRoutes = setOf(
        Routes.SETUP_ABOUT,
        Routes.SETUP_SETTINGS_CREDITS,
        Routes.SETUP_LIBRARY_SCAN,
    )
    val showSetupBar = !isPlay &&
        currentRoute !in immersiveRoutes &&
        (
            currentRoute in setupTabs ||
                currentRoute?.startsWith("setup/settings/") == true ||
                currentRoute?.startsWith("setup/scrape") == true
            )
    // ORGL Setup/Play top bar only on primary hubs — sub-screens use their own TopAppBar.
    val modeSwitcherRoutes = setupTabs + setOf(Routes.PLAY_PICKER, Routes.PLAY_FOCUS)
    val hideModeSwitcher = currentRoute !in modeSwitcherRoutes

    val rootRoutes = setOf(
        Routes.SETUP_LIBRARY,
        Routes.SETUP_SETTINGS,
        Routes.SETUP_SCRAPE,
        Routes.PLAY_PICKER,
        Routes.PLAY_FOCUS,
    )
    val canPopBack = navController.previousBackStackEntry != null &&
        currentRoute !in rootRoutes

    val setupTabSelectedIndex = setupTabIndex(currentRoute)
    val showAboutButton = !isPlay && !hideModeSwitcher

    GamepadBackHandler(canPopBack = canPopBack) {
        navController.popBackStack()
    }

    Scaffold(
        contentWindowInsets = if (hideModeSwitcher) {
            // Sub-screens own a TopAppBar; skip status-bar inset here to avoid a double gap.
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        modifier = Modifier.onPreviewKeyEvent { event ->
            when {
                showSetupBar && GamepadKeys.isShoulderLeft(event) -> {
                    navigateSetupTab(navController, cycleTabIndex(setupTabSelectedIndex, -1, 3))
                    true
                }
                showSetupBar && GamepadKeys.isShoulderRight(event) -> {
                    navigateSetupTab(navController, cycleTabIndex(setupTabSelectedIndex, 1, 3))
                    true
                }
                GamepadKeys.isUnassignedFaceButton(event) -> true
                GamepadKeys.isAbout(event) -> {
                    if (showAboutButton) navController.navigate(Routes.SETUP_ABOUT)
                    true
                }
                GamepadKeys.isGamepadBack(event) -> {
                    if (canPopBack) navController.popBackStack()
                    true
                }
                GamepadKeys.isSystemBack(event) -> {
                    if (canPopBack) navController.popBackStack()
                    true
                }
                else -> false
            }
        },
        topBar = {
            if (!hideModeSwitcher) {
                TopAppBar(
                    title = { Text(if (isPlay) "Play" else "ORGL") },
                    actions = {
                        if (!isPlay) {
                            IconButton(
                                onClick = { navController.navigate(Routes.SETUP_ABOUT) },
                                modifier = Modifier.focusProperties { canFocus = false },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "About ORGL",
                                )
                            }
                        }
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .focusProperties { canFocus = false },
                        ) {
                            SegmentedButton(
                                selected = !isPlay,
                                onClick = {
                                    shellViewModel.setMode(AppMode.SETUP)
                                    navController.navigate(Routes.SETUP_LIBRARY) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.focusProperties { canFocus = false },
                            ) { Text("Setup") }
                            SegmentedButton(
                                selected = isPlay,
                                onClick = {
                                    shellViewModel.setMode(AppMode.PLAY)
                                    val dest = if (activeCommitment != null) {
                                        Routes.PLAY_FOCUS
                                    } else {
                                        Routes.PLAY_PICKER
                                    }
                                    navController.navigate(dest) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.focusProperties { canFocus = false },
                            ) { Text("Play") }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showSetupBar) {
                OrglBottomNavStrip(
                    labels = listOf("Library", "Scrape", "Settings"),
                    selectedIndex = setupTabSelectedIndex,
                    icons = { index, _ ->
                        when (index) {
                            0 -> Icon(Icons.Default.ViewModule, contentDescription = null)
                            1 -> Icon(Icons.Default.Image, contentDescription = null)
                            else -> Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    },
                    onTabClick = { navigateSetupTab(navController, it) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
        NavHost(
            navController = navController,
            startDestination = when {
                isPlay && activeCommitment != null -> Routes.PLAY_FOCUS
                isPlay -> Routes.PLAY_PICKER
                else -> Routes.SETUP_LIBRARY
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.SETUP_LIBRARY) {
                LibraryScreen(onSystemClick = { navController.navigate(Routes.setupSystem(it)) })
            }
            composable(Routes.SETUP_ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCredits = { navController.navigate(Routes.SETUP_SETTINGS_CREDITS) },
                )
            }
            composable(Routes.SETUP_SYSTEM) {
                SystemGamesScreen(
                    onGameClick = { navController.navigate(Routes.setupGame(it)) },
                    onEmulatorSettings = { systemId ->
                        navController.navigate(Routes.setupSystemEmulator(systemId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETUP_SYSTEM_EMULATOR) {
                SystemEmulatorSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_GAME) {
                GameDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRetroAchievements = { gameId ->
                        navController.navigate(Routes.gameRetroAchievements(gameId))
                    },
                )
            }
            composable(Routes.SETUP_GAME_RA) {
                GameRetroAchievementsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Routes.SETUP_SETTINGS_RA) },
                )
            }
            composable(Routes.SETUP_SETTINGS) {
                SettingsScreen(
                    onOpenLibraryFolders = {
                        navController.navigate(Routes.SETUP_SETTINGS_FOLDERS)
                    },
                    onOpenEsde = { navController.navigate(Routes.SETUP_SETTINGS_ESDE) },
                    onOpenScreenScraper = {
                        navController.navigate(Routes.SETUP_SETTINGS_SCREENSCRAPER)
                    },
                    onOpenRetroAchievements = { navController.navigate(Routes.SETUP_SETTINGS_RA) },
                    onOpenHltb = { navController.navigate(Routes.SETUP_SETTINGS_HLTB) },
                    onOpenRetroArch = { navController.navigate(Routes.SETUP_SETTINGS_RETROARCH) },
                    onStartScan = { navController.navigate(Routes.SETUP_LIBRARY_SCAN) },
                )
            }
            composable(Routes.SETUP_SETTINGS_FOLDERS) {
                LibraryFoldersScreen(
                    onBack = { navController.popBackStack() },
                    onStartScan = { navController.navigate(Routes.SETUP_LIBRARY_SCAN) },
                )
            }
            composable(Routes.SETUP_SETTINGS_ESDE) {
                EsdeSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onStartScan = { navController.navigate(Routes.SETUP_LIBRARY_SCAN) },
                )
            }
            composable(Routes.SETUP_LIBRARY_SCAN) {
                LibraryScanScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_SCREENSCRAPER) {
                ScreenScraperSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEsde = { navController.navigate(Routes.SETUP_SETTINGS_ESDE) },
                )
            }
            composable(Routes.SETUP_SETTINGS_RA) {
                RetroAchievementsSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_HLTB) {
                HltbSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_RETROARCH) {
                RetroArchSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_CREDITS) {
                CreditsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SCRAPE) {
                ScrapeScreen(
                    onOpenEsde = { navController.navigate(Routes.SETUP_SETTINGS_ESDE) },
                )
            }
            composable(Routes.SETUP_SCRAPE_WIZARD) {
                ScrapeWizardScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PLAY_PICKER) {
                PlayPickerScreen(
                    onGameSelected = { navController.navigate(Routes.playCommit(it)) },
                )
            }
            composable(Routes.PLAY_COMMIT) {
                CommitConfirmScreen(
                    onConfirmed = {
                        navController.navigate(Routes.PLAY_FOCUS) {
                            popUpTo(Routes.PLAY_PICKER) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(Routes.PLAY_FOCUS) {
                FocusScreen(
                    onReleased = {
                        navController.navigate(Routes.PLAY_PICKER) {
                            popUpTo(Routes.PLAY_FOCUS) { inclusive = true }
                        }
                    },
                    onOpenJournal = { navController.navigate(Routes.PLAY_JOURNAL) },
                )
            }
            composable(Routes.PLAY_JOURNAL) {
                JournalScreen(onBack = { navController.popBackStack() })
            }
        }
        }
    }
}

private fun setupTabIndex(route: String?): Int = when {
    route == Routes.SETUP_LIBRARY -> 0
    route == Routes.SETUP_SCRAPE || route?.startsWith("setup/scrape") == true -> 1
    route == Routes.SETUP_SETTINGS || route?.startsWith("setup/settings/") == true -> 2
    else -> 0
}

private fun navigateSetupTab(
    navController: NavHostController,
    index: Int,
) {
    val destination = when (index) {
        0 -> Routes.SETUP_LIBRARY
        1 -> Routes.SETUP_SCRAPE
        else -> Routes.SETUP_SETTINGS
    }
    navController.navigate(destination) {
        popUpTo(Routes.SETUP_LIBRARY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
