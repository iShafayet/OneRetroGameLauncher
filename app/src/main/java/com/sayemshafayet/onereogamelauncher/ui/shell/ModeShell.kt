package com.sayemshafayet.onereogamelauncher.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.sayemshafayet.onereogamelauncher.ui.setup.EsdeSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.GameDetailScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.HltbSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.RetroAchievementsSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScrapeScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScreenScraperSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SettingsScreen
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
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val isPlay = settings.appMode == AppMode.PLAY
    val setupTabs = setOf(
        Routes.SETUP_LIBRARY,
        Routes.SETUP_SETTINGS,
        Routes.SETUP_SCRAPE,
    )
    val showSetupBar = !isPlay && (
        currentRoute in setupTabs ||
            currentRoute?.startsWith("setup/settings/") == true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPlay) "Play" else "Setup") },
                actions = {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(end = 8.dp)) {
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
                        ) { Text("Play") }
                    }
                },
            )
        },
        bottomBar = {
            if (showSetupBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETUP_LIBRARY,
                        onClick = {
                            navController.navigate(Routes.SETUP_LIBRARY) {
                                popUpTo(Routes.SETUP_LIBRARY) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.ViewModule, null) },
                        label = { Text("Library") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETUP_SCRAPE,
                        onClick = {
                            navController.navigate(Routes.SETUP_SCRAPE) {
                                popUpTo(Routes.SETUP_LIBRARY) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Image, null) },
                        label = { Text("Scrape") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETUP_SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETUP_SETTINGS) {
                                popUpTo(Routes.SETUP_LIBRARY) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = when {
                isPlay && activeCommitment != null -> Routes.PLAY_FOCUS
                isPlay -> Routes.PLAY_PICKER
                else -> Routes.SETUP_LIBRARY
            },
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SETUP_LIBRARY) {
                LibraryScreen(onSystemClick = { navController.navigate(Routes.setupSystem(it)) })
            }
            composable(Routes.SETUP_SYSTEM) {
                SystemGamesScreen(
                    onGameClick = { navController.navigate(Routes.setupGame(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETUP_GAME) {
                GameDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS) {
                SettingsScreen(
                    onOpenEsde = { navController.navigate(Routes.SETUP_SETTINGS_ESDE) },
                    onOpenScreenScraper = { navController.navigate(Routes.SETUP_SETTINGS_SCREENSCRAPER) },
                    onOpenRetroAchievements = { navController.navigate(Routes.SETUP_SETTINGS_RA) },
                    onOpenHltb = { navController.navigate(Routes.SETUP_SETTINGS_HLTB) },
                )
            }
            composable(Routes.SETUP_SETTINGS_ESDE) {
                EsdeSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_SCREENSCRAPER) {
                ScreenScraperSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_RA) {
                RetroAchievementsSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SETTINGS_HLTB) {
                HltbSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_SCRAPE) { ScrapeScreen() }

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
            composable(Routes.PLAY_JOURNAL) { JournalScreen() }
        }
    }
}
