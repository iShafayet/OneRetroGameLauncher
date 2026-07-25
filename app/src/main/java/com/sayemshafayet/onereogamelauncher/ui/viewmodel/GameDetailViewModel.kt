package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameConfigEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.prefs.retroAchievementsConfigured
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.HltbEstimate
import com.sayemshafayet.onereogamelauncher.domain.HltbUiPhase
import com.sayemshafayet.onereogamelauncher.domain.RaButtonState
import com.sayemshafayet.onereogamelauncher.domain.RaResult
import com.sayemshafayet.onereogamelauncher.domain.RaVisualState
import com.sayemshafayet.onereogamelauncher.hltb.HltbLookupResult
import com.sayemshafayet.onereogamelauncher.hltb.HowLongToBeatClient
import com.sayemshafayet.onereogamelauncher.launch.EmulatorLauncher
import com.sayemshafayet.onereogamelauncher.launch.LaunchResolver
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.play.PlayStatsTracker
import com.sayemshafayet.onereogamelauncher.ra.RaSupportEvaluator
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
import com.sayemshafayet.onereogamelauncher.ra.RomHashCalculator
import com.sayemshafayet.onereogamelauncher.scrape.ScrapeForegroundService
import com.sayemshafayet.onereogamelauncher.systems.SystemConfigLoader
import com.sayemshafayet.onereogamelauncher.ui.input.cycleTabIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameLaunchConfigUi(
    val useOverride: Boolean = false,
    val emulatorKey: String = "",
    val core: String = "",
    val customConfigPath: String = "",
    val emulatorChoices: List<EmulatorChoice> = emptyList(),
    val coreChoices: List<CoreChoice> = emptyList(),
    val systemEmulatorLabel: String = "",
    val systemCoreLabel: String = "",
)

data class GameHltbUiState(
    val enabled: Boolean = true,
    val phase: HltbUiPhase = HltbUiPhase.Loading,
    val estimate: HltbEstimate? = null,
)

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val commitmentRepository: CommitmentRepository,
    private val launchResolver: LaunchResolver,
    private val playStatsTracker: PlayStatsTracker,
    private val settingsRepository: SettingsRepository,
    private val emulatorLauncher: EmulatorLauncher,
    private val systemConfigLoader: SystemConfigLoader,
    private val raClient: RetroAchievementsClient,
    private val raSupportEvaluator: RaSupportEvaluator,
    private val romHashCalculator: RomHashCalculator,
    private val hltbClient: HowLongToBeatClient,
) : ViewModel() {
    val gameId: Long = savedStateHandle.get<String>("gameId")?.toLongOrNull() ?: 0L

    val game: StateFlow<GameEntity?> = libraryRepository.observeGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val media: StateFlow<List<MediaEntity>> = libraryRepository.observeMedia(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val config: StateFlow<GameConfigEntity?> = libraryRepository.observeGameConfig(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeCommitmentForGame = commitmentRepository.observeActiveForGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activePlayRunCount = commitmentRepository.observeAllActive()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** True when launching from Setup should show the debug/test guard (not this game's Play slot). */
    val needsDebugLaunchGuard: StateFlow<Boolean> = combine(
        activeCommitmentForGame,
        commitmentRepository.observeAllActive(),
    ) { forGame, active ->
        forGame == null && active.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _launchConfig = MutableStateFlow(GameLaunchConfigUi())
    val launchConfig: StateFlow<GameLaunchConfigUi> = _launchConfig.asStateFlow()

    private val _commitmentPlaytimeMs = MutableStateFlow(0L)
    val commitmentPlaytimeMs: StateFlow<Long> = _commitmentPlaytimeMs.asStateFlow()

    private val _raUi = MutableStateFlow(GameRaUiState())
    val raUi: StateFlow<GameRaUiState> = _raUi.asStateFlow()

    private val _hltbUi = MutableStateFlow(GameHltbUiState())
    val hltbUi: StateFlow<GameHltbUiState> = _hltbUi.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private var systemCache: SystemEntity? = null
    private var lastHltbTitle: String? = null

    init {
        viewModelScope.launch {
            config.collect { cfg ->
                syncFromConfig(cfg)
            }
        }
        viewModelScope.launch {
            game.collect { g ->
                if (g != null) {
                    reloadChoices(g.systemId)
                    refreshCommitmentPlaytime()
                    refreshRaStatus(g)
                    refreshHltb(g)
                }
            }
        }
        viewModelScope.launch {
            launchConfig.collect { refreshRaStatus(game.value) }
        }
    }

    private fun refreshRaStatus(game: GameEntity?) {
        if (game == null) return
        viewModelScope.launch {
            _raUi.update { it.copy(loading = true) }
            val settings = settingsRepository.settings.first()
            val signedIn = settings.retroAchievementsConfigured()
            if (!signedIn) {
                _raUi.value = GameRaUiState(
                    loading = false,
                    button = RaButtonState(
                        visual = RaVisualState.SIGN_IN_REQUIRED,
                        subtitle = "Sign in under Settings → RetroAchievements",
                        enabled = true,
                    ),
                )
                return@launch
            }

            val lookupResult = raClient.lookupGame(
                settings = settings,
                romPath = game.romPath,
                title = game.title,
                systemFolder = systemCache?.folderName,
                knownGameId = game.raGameId,
                romPathsJson = game.romPathsJson,
            )
            when (lookupResult) {
                is RaResult.Ok -> libraryRepository.saveRaGameId(gameId, lookupResult.value.gameId)
                is RaResult.Unsupported -> libraryRepository.clearRaGameId(gameId)
                is RaResult.Failed -> { /* keep cached id on transient errors */ }
            }

            val effectiveEmulator = if (_launchConfig.value.useOverride) {
                _launchConfig.value.emulatorKey
            } else {
                systemCache?.defaultEmulatorKey ?: _launchConfig.value.emulatorKey
            }

            _raUi.value = GameRaUiState(
                loading = false,
                button = raSupportEvaluator.evaluateButton(
                    signedIn = true,
                    lookupResult = lookupResult,
                    effectiveEmulatorKey = effectiveEmulator,
                    romHashable = romHashCalculator.isHashable(
                        RomHashCalculator.RomAccess(
                            romPath = game.romPath,
                            romPathsJson = game.romPathsJson,
                            systemFolder = systemCache?.folderName,
                            romsDirPath = settings.romsDirPath,
                            romsTreeUri = settings.romsDirUri,
                        ),
                    ),
                ),
            )
        }
    }

    fun onScreenResume() {
        viewModelScope.launch {
            playStatsTracker.onAppForeground()
            refreshCommitmentPlaytime()
            refreshRaStatus(game.value)
            game.value?.let { refreshHltb(it, force = false) }
        }
    }

    private fun refreshHltb(game: GameEntity, force: Boolean = false) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.hltbEnabled) {
                lastHltbTitle = null
                _hltbUi.value = GameHltbUiState(
                    enabled = false,
                    phase = HltbUiPhase.Missing,
                    estimate = null,
                )
                return@launch
            }
            val title = game.title.trim()
            if (!force &&
                title == lastHltbTitle &&
                _hltbUi.value.enabled &&
                _hltbUi.value.phase != HltbUiPhase.Loading
            ) {
                return@launch
            }
            lastHltbTitle = title
            _hltbUi.value = GameHltbUiState(
                enabled = true,
                phase = HltbUiPhase.Loading,
                estimate = _hltbUi.value.estimate,
            )
            when (val result = hltbClient.search(title)) {
                is HltbLookupResult.Found -> {
                    _hltbUi.value = GameHltbUiState(
                        enabled = true,
                        phase = HltbUiPhase.Ready,
                        estimate = result.estimate,
                    )
                }
                HltbLookupResult.NotFound -> {
                    _hltbUi.value = GameHltbUiState(
                        enabled = true,
                        phase = HltbUiPhase.Missing,
                        estimate = null,
                    )
                }
                is HltbLookupResult.Failed -> {
                    _hltbUi.value = GameHltbUiState(
                        enabled = true,
                        phase = HltbUiPhase.Error,
                        estimate = null,
                    )
                }
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index.coerceIn(0, GAME_DETAIL_TAB_COUNT - 1)
    }

    fun cycleSelectedTab(delta: Int) {
        _selectedTab.update { cycleTabIndex(it, delta, GAME_DETAIL_TAB_COUNT) }
    }

    private suspend fun refreshCommitmentPlaytime() {
        _commitmentPlaytimeMs.value = commitmentRepository.totalPlaytimeMsForGame(gameId)
    }

    suspend fun system(): SystemEntity? {
        systemCache?.let { return it }
        val g = libraryRepository.getGame(gameId) ?: return null
        systemCache = libraryRepository.getSystem(g.systemId)
        return systemCache
    }

    suspend fun launchAllowed(): Boolean = commitmentRepository.isLaunchAllowed(gameId)

    suspend fun launch(): String? = launchResolver.launch(gameId)

    fun toggleFavorite() {
        viewModelScope.launch { libraryRepository.toggleFavorite(gameId) }
    }

    fun toggleFinished() {
        viewModelScope.launch {
            val g = libraryRepository.getGame(gameId) ?: return@launch
            if (g.completedStatus == GameCompletedStatus.FINISHED) {
                libraryRepository.updateGameMetadata(gameId, clearCompleted = true)
            } else {
                libraryRepository.updateGameMetadata(
                    gameId,
                    completedStatus = GameCompletedStatus.FINISHED,
                )
            }
        }
    }

    fun toggleDropped() {
        viewModelScope.launch {
            val g = libraryRepository.getGame(gameId) ?: return@launch
            if (g.completedStatus == GameCompletedStatus.DROPPED) {
                libraryRepository.updateGameMetadata(gameId, clearCompleted = true)
            } else {
                libraryRepository.updateGameMetadata(
                    gameId,
                    completedStatus = GameCompletedStatus.DROPPED,
                )
            }
        }
    }

    fun setUseOverride(enabled: Boolean) {
        _launchConfig.update { it.copy(useOverride = enabled) }
        persistLaunchConfig()
    }

    fun setEmulatorKey(key: String) {
        _launchConfig.update {
            it.copy(
                emulatorKey = key,
                core = if (key.equals("RETROARCH", ignoreCase = true)) it.core else "",
            )
        }
    }

    fun setCore(core: String) {
        _launchConfig.update { it.copy(core = core) }
    }

    fun setCustomConfigPath(path: String) {
        _launchConfig.update { it.copy(customConfigPath = path) }
    }

    fun saveLaunchConfig() {
        persistLaunchConfig()
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            libraryRepository.saveGameNotes(gameId, notes)
        }
    }

    fun scrapeGame() {
        viewModelScope.launch {
            val g = libraryRepository.getGame(gameId) ?: return@launch
            val sys = libraryRepository.getSystem(g.systemId) ?: return@launch
            ScrapeForegroundService.scrapeGame(context, gameId, sys.folderName)
        }
    }

    private fun persistLaunchConfig() {
        viewModelScope.launch {
            val state = _launchConfig.value
            libraryRepository.saveGameConfig(
                gameId = gameId,
                useOverride = state.useOverride,
                emulatorKey = state.emulatorKey.ifBlank { null },
                coreOverride = state.core.ifBlank { null },
                customConfigPath = state.customConfigPath.ifBlank { null },
            )
        }
    }

    private suspend fun syncFromConfig(cfg: GameConfigEntity?) {
        val current = _launchConfig.value
        _launchConfig.update {
            it.copy(
                useOverride = cfg?.useOverride == true,
                emulatorKey = cfg?.emulatorKey?.takeIf { k -> k.isNotBlank() }
                    ?: current.emulatorKey,
                core = cfg?.coreOverride?.takeIf { c -> c.isNotBlank() } ?: current.core,
                customConfigPath = cfg?.customConfigPath.orEmpty(),
            )
        }
    }

    private suspend fun reloadChoices(systemId: Long) {
        val sys = libraryRepository.getSystem(systemId) ?: return
        systemCache = sys
        val settings = settingsRepository.current()
        val def = systemConfigLoader.systemByFolder(sys.folderName)
        val fromCommands = def?.let { systemConfigLoader.emulatorOptionsForSystem(it) }.orEmpty()
        val choices = buildEmulatorChoices(fromCommands, settings.preferredRetroArchPackage)
        val cores = def?.let { systemConfigLoader.retroArchCoresForSystem(it) }
            .orEmpty()
            .map { (label, file) -> CoreChoice(fileName = file, label = label) }

        val systemEmu = sys.defaultEmulatorKey?.takeIf { it.isNotBlank() }
            ?: choices.firstOrNull { it.installed }?.key
            ?: ""
        val systemCore = usableCoreFile(sys.defaultCore)
            ?: cores.firstOrNull()?.fileName
            ?: ""

        val cfg = config.value
        val emuKey = cfg?.emulatorKey?.takeIf { it.isNotBlank() }
            ?: systemEmu
        val core = usableCoreFile(cfg?.coreOverride) ?: systemCore

        _launchConfig.update {
            it.copy(
                useOverride = cfg?.useOverride == true,
                emulatorKey = emuKey,
                core = core,
                customConfigPath = cfg?.customConfigPath.orEmpty(),
                emulatorChoices = choices,
                coreChoices = cores,
                systemEmulatorLabel = choices.firstOrNull {
                    it.key.equals(systemEmu, ignoreCase = true)
                }?.label ?: systemEmu.ifBlank { "Not set" },
                systemCoreLabel = cores.firstOrNull { it.fileName == systemCore }?.label
                    ?: systemCore.ifBlank { "Not set" },
            )
        }
    }

    private fun usableCoreFile(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = com.sayemshafayet.onereogamelauncher.systems.LibretroCorePaths.coreFileNameFromExtra(raw)
        return normalized.takeIf { it.endsWith(".so", ignoreCase = true) }
    }

    private fun buildEmulatorChoices(
        catalog: List<Pair<String, String>>,
        preferredRa: String,
    ): List<EmulatorChoice> {
        val byKey = linkedMapOf<String, EmulatorChoice>()

        fun add(key: String, labelHint: String) {
            val canonical = when {
                key.equals("RETROARCH", ignoreCase = true) -> "RETROARCH"
                else -> emulatorLauncher.profileForKey(key)?.key ?: return
            }
            if (byKey.containsKey(canonical)) return
            val label = when (canonical) {
                "RETROARCH" -> "RetroArch"
                else -> emulatorLauncher.profileForKey(canonical)?.displayName ?: labelHint.ifBlank { canonical }
            }
            val installed = emulatorLauncher.installedForKey(canonical, preferredRa) != null
            byKey[canonical] = EmulatorChoice(canonical, label, installed)
        }

        for ((key, label) in catalog) add(key, label)
        add("RETROARCH", "RetroArch")
        for (profile in EmulatorLauncher.SUPPORTED_PROFILES) {
            if (emulatorLauncher.installedForKey(profile.key, preferredRa) != null) {
                add(profile.key, profile.displayName)
            }
        }
        return byKey.values.sortedWith(
            compareByDescending<EmulatorChoice> { it.installed }.thenBy { it.label.lowercase() },
        )
    }
}

const val GAME_DETAIL_TAB_COUNT = 3
