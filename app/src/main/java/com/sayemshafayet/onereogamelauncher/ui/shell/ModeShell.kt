package com.sayemshafayet.onereogamelauncher.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import com.sayemshafayet.onereogamelauncher.R
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadBackHandler
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadKeys
import com.sayemshafayet.onereogamelauncher.ui.input.ShellHardwareKeys
import com.sayemshafayet.onereogamelauncher.ui.input.isSoftKeyboardVisible
import com.sayemshafayet.onereogamelauncher.ui.input.rememberDoublePressConfirmHandler
import com.sayemshafayet.onereogamelauncher.ui.input.rememberDoublePressExitHandler
import com.sayemshafayet.onereogamelauncher.ui.input.OrglBottomNavStrip
import com.sayemshafayet.onereogamelauncher.ui.input.cycleTabIndex
import com.sayemshafayet.onereogamelauncher.ui.theme.SelectionIndicator
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sayemshafayet.onereogamelauncher.data.db.entity.CommitmentEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.domain.AppMode
import com.sayemshafayet.onereogamelauncher.ui.navigation.Routes
import com.sayemshafayet.onereogamelauncher.ui.play.CommitConfirmScreen
import com.sayemshafayet.onereogamelauncher.ui.play.FocusScreen
import com.sayemshafayet.onereogamelauncher.ui.play.PlayCompletionScreen
import com.sayemshafayet.onereogamelauncher.ui.play.PlayPickerScreen
import com.sayemshafayet.onereogamelauncher.ui.play.PlaySlotBar
import com.sayemshafayet.onereogamelauncher.ui.setup.AboutScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibrarySearchScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.CreditsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.EsdeSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.GameRetroAchievementsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.GameDetailScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.MediaViewerScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.HistoryScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.HltbSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryFoldersScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryScanScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.LibraryScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.rememberIsLibraryWideLandscape
import com.sayemshafayet.onereogamelauncher.ui.setup.PlaySlotsSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.RetroAchievementsSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.RetroArchSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScrapeWizardScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.ScreenScraperSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.DatabaseSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SystemEmulatorSettingsScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SystemGamesScreen
import com.sayemshafayet.onereogamelauncher.ui.setup.SystemInfoScreen
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameDetailViewModel
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
    val activeCommitments by mainViewModel.activeCommitments.collectAsState()
    val visibleActiveCommitments = activeCommitments.filter {
        it.slotIndex in 0 until settings.playSlotCount
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val isPlay = settings.appMode == AppMode.PLAY
    val setupTabs = setOf(
        Routes.SETUP_LIBRARY,
        Routes.SETUP_SETTINGS,
        Routes.SETUP_HISTORY,
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
    // Primary hubs use the mode switcher in the shell top bar; all routes share that bar slot.
    val modeSwitcherRoutes = setupTabs + setOf(Routes.PLAY_PICKER, Routes.PLAY_FOCUS)
    val isHubRoute = currentRoute in setupTabs || Routes.isPlayHubRoute(currentRoute)

    val rootRoutes = setOf(
        Routes.SETUP_LIBRARY,
        Routes.SETUP_SETTINGS,
        Routes.SETUP_HISTORY,
        Routes.PLAY_COMPLETE,
    )
    val canPopBack = navController.previousBackStackEntry != null &&
        currentRoute !in rootRoutes &&
        !isPlayRootRoute(currentRoute)

    val canDoublePressExit = isModeRootRoute(isPlay, currentRoute)
    val activity = LocalActivity.current
    val onDoublePressExit = rememberDoublePressExitHandler(
        resetKey = currentRoute,
        message = stringResource(R.string.press_again_to_exit),
        onExit = { activity?.finish() },
    )
    fun handleBackPress() {
        when {
            canPopBack -> navController.popBackStack()
            canDoublePressExit -> onDoublePressExit()
        }
    }

    var showSetupGuard by remember { mutableStateOf(false) }
    fun switchToSetup() {
        shellViewModel.setMode(AppMode.SETUP)
        navController.navigate(Routes.SETUP_LIBRARY) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun switchToPlay() {
        shellViewModel.setMode(AppMode.PLAY)
        navController.navigate(Routes.playEntryHub(visibleActiveCommitments)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
    fun requestSetupMode() {
        if (isPlay && visibleActiveCommitments.isNotEmpty()) {
            showSetupGuard = true
        } else {
            switchToSetup()
        }
    }
    val onDoublePressModeSwitch = rememberDoublePressConfirmHandler(
        resetKey = currentRoute to isPlay,
        message = stringResource(
            if (isPlay) R.string.press_again_to_setup else R.string.press_again_to_play,
        ),
        onConfirm = {
            if (isPlay) requestSetupMode() else switchToPlay()
        },
    )

    val setupTabSelectedIndex = setupTabIndex(currentRoute)
    val libraryWideLandscape = rememberIsLibraryWideLandscape()
    val canLibrarySearch = !isPlay && currentRoute == Routes.SETUP_LIBRARY
    val shellView = LocalView.current
    val shellKeyboard = LocalSoftwareKeyboardController.current
    val shellFocusManager = LocalFocusManager.current

    if (showSetupGuard) {
        PlayToSetupGuardDialog(
            activeRunCount = visibleActiveCommitments.size,
            onDismiss = { showSetupGuard = false },
            onConfirmed = {
                showSetupGuard = false
                switchToSetup()
            },
        )
    }

    GamepadBackHandler(
        canPopBack = canPopBack,
        onBack = { navController.popBackStack() },
        onRootBack = if (canDoublePressExit) onDoublePressExit else null,
    )

    // Keep start destinations stable — tying them to activeCommitment resets the graph when a
    // run ends and would skip the completion screen.
    val navStartDestination = if (isPlay) Routes.playPicker(0) else Routes.SETUP_LIBRARY

    LaunchedEffect(isPlay, visibleActiveCommitments, settings.playSlotCount, backStack?.id, currentRoute) {
        if (!isPlay) return@LaunchedEffect
        if (currentRoute in playFlowRoutes) return@LaunchedEffect
        if (!Routes.isPlayHubRoute(currentRoute)) return@LaunchedEffect
        val slot = Routes.slotIndexFromEntry(backStack)
            .coerceIn(0, (settings.playSlotCount - 1).coerceAtLeast(0))
        syncPlayHubForSlot(navController, backStack, slot, visibleActiveCommitments)
    }

    val showPlaySlotBar = isPlay &&
        settings.playSlotCount > 1 &&
        Routes.isPlayHubRoute(currentRoute)
    val currentPlaySlot = Routes.slotIndexFromEntry(backStack)
        .coerceIn(0, (settings.playSlotCount - 1).coerceAtLeast(0))

    val isGameDetailRoute = currentRoute == Routes.SETUP_GAME
    val gameDetailViewModel: GameDetailViewModel? =
        if (isGameDetailRoute && backStack != null) hiltViewModel(backStack!!) else null

    val shoulderContext = rememberUpdatedState(
        ShoulderKeyContext(
            gameDetailViewModel = gameDetailViewModel,
            showSetupBar = showSetupBar,
            showPlaySlotBar = showPlaySlotBar,
            setupTabSelectedIndex = setupTabSelectedIndex,
            playSlotCount = settings.playSlotCount,
            currentPlaySlot = currentPlaySlot,
            visibleActiveCommitments = visibleActiveCommitments,
            backStack = backStack,
        ),
    )
    DisposableEffect(navController) {
        val handler = handler@{ event: AndroidKeyEvent ->
            if (!ShellHardwareKeys.isShoulderKey(event.keyCode)) return@handler false
            val ctx = shoulderContext.value
            val canHandle = ctx.gameDetailViewModel != null || ctx.showSetupBar || ctx.showPlaySlotBar
            if (!canHandle) return@handler false
            // Consume down+up so focused lists never steal PageUp/PageDown; act only on up.
            if (event.action == AndroidKeyEvent.ACTION_UP) {
                val left = ShellHardwareKeys.isShoulderLeft(event.keyCode)
                val right = ShellHardwareKeys.isShoulderRight(event.keyCode)
                when {
                    ctx.gameDetailViewModel != null && left ->
                        ctx.gameDetailViewModel.cycleSelectedTab(-1)
                    ctx.gameDetailViewModel != null && right ->
                        ctx.gameDetailViewModel.cycleSelectedTab(1)
                    ctx.showSetupBar && left ->
                        navigateSetupTab(
                            navController,
                            cycleTabIndex(ctx.setupTabSelectedIndex, -1, 3),
                        )
                    ctx.showSetupBar && right ->
                        navigateSetupTab(
                            navController,
                            cycleTabIndex(ctx.setupTabSelectedIndex, 1, 3),
                        )
                    ctx.showPlaySlotBar && left ->
                        navigatePlaySlot(
                            navController,
                            ctx.backStack,
                            ctx.visibleActiveCommitments,
                            ctx.playSlotCount,
                            ctx.currentPlaySlot,
                            -1,
                        )
                    ctx.showPlaySlotBar && right ->
                        navigatePlaySlot(
                            navController,
                            ctx.backStack,
                            ctx.visibleActiveCommitments,
                            ctx.playSlotCount,
                            ctx.currentPlaySlot,
                            1,
                        )
                }
            }
            true
        }
        ShellHardwareKeys.shoulderHandler = handler
        onDispose {
            if (ShellHardwareKeys.shoulderHandler === handler) {
                ShellHardwareKeys.shoulderHandler = null
            }
        }
    }

    DisposableEffect(canLibrarySearch) {
        if (!canLibrarySearch) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }
        val registered: (AndroidKeyEvent) -> Boolean = { event ->
            if (event.action == AndroidKeyEvent.ACTION_UP) {
                navController.navigate(Routes.SETUP_LIBRARY_SEARCH)
                true
            } else {
                false
            }
        }
        ShellHardwareKeys.defaultButtonXHandler = registered
        onDispose {
            if (ShellHardwareKeys.defaultButtonXHandler === registered) {
                ShellHardwareKeys.defaultButtonXHandler = null
            }
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            when {
                GamepadKeys.isModeToggle(event) -> {
                    if (canDoublePressExit) onDoublePressModeSwitch()
                    true
                }
                GamepadKeys.isGamepadBack(event) || GamepadKeys.isSystemBack(event) -> {
                    if (shellView.isSoftKeyboardVisible()) {
                        shellFocusManager.clearFocus(force = true)
                        shellKeyboard?.hide()
                        shellView.post { shellKeyboard?.hide() }
                        shellView.postDelayed({ shellKeyboard?.hide() }, 80)
                    } else {
                        handleBackPress()
                    }
                    true
                }
                else -> false
            }
        },
        topBar = {
            Column {
                OrglShellTopBar(
                    navController = navController,
                    backStackEntry = backStack,
                    currentRoute = currentRoute,
                    isPlay = isPlay,
                    canPopBack = canPopBack,
                    onRequestSetupMode = ::requestSetupMode,
                    onRequestPlayMode = ::switchToPlay,
                )
                if (showPlaySlotBar) {
                    PlaySlotBar(
                        slotCount = settings.playSlotCount,
                        currentSlot = currentPlaySlot,
                        occupiedSlots = visibleActiveCommitments.map { it.slotIndex }.toSet(),
                        onPrevious = {
                            navigatePlaySlot(
                                navController,
                                backStack,
                                visibleActiveCommitments,
                                settings.playSlotCount,
                                currentPlaySlot,
                                -1,
                            )
                        },
                        onNext = {
                            navigatePlaySlot(
                                navController,
                                backStack,
                                visibleActiveCommitments,
                                settings.playSlotCount,
                                currentPlaySlot,
                                1,
                            )
                        },
                    )
                }
            }
        },
        bottomBar = {
            if (showSetupBar) {
                OrglBottomNavStrip(
                    labels = listOf("Library", "History", "Settings"),
                    selectedIndex = setupTabSelectedIndex,
                    iconsOnly = libraryWideLandscape,
                    icons = { index, selected ->
                        val tint = if (selected) {
                            SelectionIndicator
                        } else {
                            LocalContentColor.current.copy(alpha = 0.72f)
                        }
                        when (index) {
                            0 -> Icon(Icons.Default.ViewModule, contentDescription = null, tint = tint)
                            1 -> Icon(Icons.Default.History, contentDescription = null, tint = tint)
                            else -> Icon(Icons.Default.Settings, contentDescription = null, tint = tint)
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
            startDestination = navStartDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.SETUP_LIBRARY) {
                LibraryScreen(onSystemClick = { navController.navigate(Routes.setupSystem(it)) })
            }
            composable(Routes.SETUP_LIBRARY_SEARCH) {
                LibrarySearchScreen(
                    onGameClick = { navController.navigate(Routes.setupGame(it)) },
                )
            }
            composable(Routes.SETUP_ABOUT) {
                AboutScreen(
                    onOpenCredits = { navController.navigate(Routes.SETUP_SETTINGS_CREDITS) },
                )
            }
            composable(Routes.SETUP_SYSTEM) {
                SystemGamesScreen(
                    onGameClick = { navController.navigate(Routes.setupGame(it)) },
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
                    onOpenMedia = { gameId, mediaId ->
                        navController.navigate(Routes.gameMedia(gameId, mediaId))
                    },
                )
            }
            composable(
                route = Routes.SETUP_GAME_MEDIA,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.LongType },
                    navArgument("mediaId") { type = NavType.LongType },
                ),
            ) {
                MediaViewerScreen()
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
                    onOpenPlaySlots = { navController.navigate(Routes.SETUP_SETTINGS_PLAY_SLOTS) },
                    onOpenDatabase = { navController.navigate(Routes.SETUP_SETTINGS_DATABASE) },
                    onOpenSystemInfo = {
                        navController.navigate(Routes.SETUP_SETTINGS_SYSTEM_INFO)
                    },
                    onOpenAbout = { navController.navigate(Routes.SETUP_ABOUT) },
                    onOpenCredits = { navController.navigate(Routes.SETUP_SETTINGS_CREDITS) },
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
            composable(Routes.SETUP_SETTINGS_PLAY_SLOTS) {
                PlaySlotsSettingsScreen()
            }
            composable(Routes.SETUP_SETTINGS_DATABASE) {
                DatabaseSettingsScreen()
            }
            composable(Routes.SETUP_SETTINGS_SYSTEM_INFO) {
                SystemInfoScreen()
            }
            composable(Routes.SETUP_SETTINGS_CREDITS) {
                CreditsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETUP_HISTORY) {
                HistoryScreen(
                    onOpenRun = { navController.navigate(Routes.historyRun(it)) },
                )
            }
            composable(Routes.SETUP_HISTORY_RUN) {
                PlayCompletionScreen(
                    onBack = { navController.popBackStack() },
                    onMissingData = { navController.popBackStack() },
                )
            }
            composable(Routes.SETUP_SCRAPE_WIZARD) {
                ScrapeWizardScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.PLAY_PICKER,
                arguments = listOf(
                    navArgument("slotIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val slotIndex = entry.arguments?.getInt("slotIndex") ?: 0
                PlayPickerScreen(
                    onGameSelected = { navController.navigate(Routes.playCommit(it, slotIndex)) },
                    viewModel = hiltViewModel(entry),
                )
            }
            composable(
                route = Routes.PLAY_COMMIT,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.LongType },
                    navArgument("slotIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val slotIndex = entry.arguments?.getInt("slotIndex") ?: 0
                CommitConfirmScreen(
                    onConfirmed = {
                        navController.navigate(Routes.playFocus(slotIndex)) {
                            popUpTo(Routes.PLAY_PICKER) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PLAY_FOCUS,
                arguments = listOf(
                    navArgument("slotIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val slotIndex = entry.arguments?.getInt("slotIndex") ?: 0
                FocusScreen(
                    onRunCompleted = {
                        navController.navigate(Routes.playComplete(slotIndex)) {
                            popUpTo(Routes.playFocus(slotIndex)) { inclusive = true }
                        }
                    },
                    onPickGame = {
                        navController.navigate(Routes.playPicker(slotIndex)) {
                            popUpTo(Routes.PLAY_FOCUS) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenJournal = { navController.navigate(Routes.SETUP_HISTORY) },
                    onOpenRetroAchievements = { gameId ->
                        navController.navigate(Routes.gameRetroAchievements(gameId))
                    },
                    viewModel = hiltViewModel(entry),
                )
            }
            composable(
                route = Routes.PLAY_COMPLETE,
                arguments = listOf(
                    navArgument("slotIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val slotIndex = entry.arguments?.getInt("slotIndex") ?: 0
                PlayCompletionScreen(
                    onStartNewAdventure = {
                        navController.navigate(Routes.playPicker(slotIndex)) {
                            popUpTo(Routes.playComplete(slotIndex)) { inclusive = true }
                        }
                    },
                    onMissingData = {
                        navController.navigate(Routes.playPicker(slotIndex)) {
                            popUpTo(Routes.playComplete(slotIndex)) { inclusive = true }
                        }
                    },
                )
            }
        }
        }
    }
}

private val playFlowRoutes = setOf(
    Routes.PLAY_COMMIT,
    Routes.PLAY_COMPLETE,
    Routes.SETUP_HISTORY,
)

private fun isPlayRootRoute(route: String?): Boolean =
    route == Routes.PLAY_PICKER ||
        route == Routes.PLAY_FOCUS ||
        route?.startsWith("play/picker/") == true ||
        route?.startsWith("play/focus/") == true ||
        route?.startsWith("play/complete/") == true

private fun isModeRootRoute(isPlay: Boolean, route: String?): Boolean = when {
    isPlay -> Routes.isPlayHubRoute(route) ||
        route?.startsWith("play/picker/") == true ||
        route?.startsWith("play/focus/") == true
    else -> route == Routes.SETUP_LIBRARY ||
        route == Routes.SETUP_SETTINGS ||
        route == Routes.SETUP_HISTORY
}

private fun syncPlayHubForSlot(
    navController: NavHostController,
    currentEntry: NavBackStackEntry?,
    slot: Int,
    activeCommitments: List<CommitmentEntity>,
) {
    val hubRoute = currentEntry?.destination?.route ?: return
    if (hubRoute != Routes.PLAY_PICKER && hubRoute != Routes.PLAY_FOCUS) return

    val occupied = activeCommitments.any { it.slotIndex == slot }
    val wrongHub = (occupied && hubRoute == Routes.PLAY_PICKER) ||
        (!occupied && hubRoute == Routes.PLAY_FOCUS)
    if (!wrongHub) return

    navigatePlayHub(
        navController = navController,
        currentEntry = currentEntry,
        slot = slot,
        activeCommitments = activeCommitments,
    )
}

private fun navigatePlayHub(
    navController: NavHostController,
    currentEntry: NavBackStackEntry?,
    slot: Int,
    activeCommitments: List<CommitmentEntity>,
) {
    val occupied = activeCommitments.any { it.slotIndex == slot }
    val dest = Routes.playHubForSlot(slot, occupied)
    val hubRoute = currentEntry?.destination?.route
    navController.navigate(dest) {
        if (hubRoute == Routes.PLAY_FOCUS || hubRoute == Routes.PLAY_PICKER) {
            popUpTo(hubRoute) { inclusive = true }
        }
        launchSingleTop = true
    }
}

private fun navigatePlaySlot(
    navController: NavHostController,
    currentEntry: NavBackStackEntry?,
    activeCommitments: List<CommitmentEntity>,
    slotCount: Int,
    currentSlot: Int,
    delta: Int,
) {
    if (slotCount <= 0) return
    val next = (currentSlot + delta).floorMod(slotCount)
    navigatePlayHub(navController, currentEntry, next, activeCommitments)
}

private fun Int.floorMod(mod: Int): Int {
    val r = this % mod
    return if (r < 0) r + mod else r
}

private data class ShoulderKeyContext(
    val gameDetailViewModel: GameDetailViewModel?,
    val showSetupBar: Boolean,
    val showPlaySlotBar: Boolean,
    val setupTabSelectedIndex: Int,
    val playSlotCount: Int,
    val currentPlaySlot: Int,
    val visibleActiveCommitments: List<CommitmentEntity>,
    val backStack: NavBackStackEntry?,
)

private fun setupTabIndex(route: String?): Int = when {
    route == Routes.SETUP_LIBRARY -> 0
    route == Routes.SETUP_HISTORY || route?.startsWith("setup/history/") == true -> 1
    route == Routes.SETUP_SETTINGS || route?.startsWith("setup/settings/") == true -> 2
    else -> 0
}

private fun navigateSetupTab(
    navController: NavHostController,
    index: Int,
) {
    val destination = when (index) {
        0 -> Routes.SETUP_LIBRARY
        1 -> Routes.SETUP_HISTORY
        else -> Routes.SETUP_SETTINGS
    }
    navController.navigate(destination) {
        popUpTo(Routes.SETUP_LIBRARY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
