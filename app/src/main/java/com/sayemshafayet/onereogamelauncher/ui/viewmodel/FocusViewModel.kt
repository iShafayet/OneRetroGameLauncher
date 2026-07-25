package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.CommitmentEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.prefs.retroAchievementsConfigured
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.domain.HltbEstimate
import com.sayemshafayet.onereogamelauncher.domain.HltbUiPhase
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.RaButtonState
import com.sayemshafayet.onereogamelauncher.domain.RaResult
import com.sayemshafayet.onereogamelauncher.domain.RaVisualState
import com.sayemshafayet.onereogamelauncher.hltb.HltbLookupResult
import com.sayemshafayet.onereogamelauncher.hltb.HowLongToBeatClient
import com.sayemshafayet.onereogamelauncher.launch.LaunchResolver
import com.sayemshafayet.onereogamelauncher.play.PlayStatsTracker
import com.sayemshafayet.onereogamelauncher.play.CollageInput
import com.sayemshafayet.onereogamelauncher.play.RunCardStore
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionData
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionStore
import com.sayemshafayet.onereogamelauncher.ra.RaSupportEvaluator
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
import com.sayemshafayet.onereogamelauncher.ra.RomHashCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val slotIndex: Int = 0,
    val commitment: CommitmentEntity? = null,
    val game: GameEntity? = null,
    val system: SystemEntity? = null,
    val media: List<MediaEntity> = emptyList(),
    val playtimeMs: Long = 0,
    val sessionCount: Int = 0,
    val hltb: HltbEstimate? = null,
    val hltbEnabled: Boolean = true,
    val hltbPhase: HltbUiPhase = HltbUiPhase.Loading,
    val launchError: String? = null,
    val loadingExtras: Boolean = false,
    val extrasLoaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val commitmentRepository: CommitmentRepository,
    private val libraryRepository: LibraryRepository,
    private val launchResolver: LaunchResolver,
    private val settingsRepository: SettingsRepository,
    private val hltbClient: HowLongToBeatClient,
    private val raClient: RetroAchievementsClient,
    private val raSupportEvaluator: RaSupportEvaluator,
    private val romHashCalculator: RomHashCalculator,
    private val runCardStore: RunCardStore,
    private val playCompletionStore: PlayCompletionStore,
    private val playStatsTracker: PlayStatsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private val _raUi = MutableStateFlow(GameRaUiState())
    val raUi: StateFlow<GameRaUiState> = _raUi.asStateFlow()

    private var launchedSession = false

    init {
        viewModelScope.launch {
            savedStateHandle.getStateFlow("slotIndex", 0)
                .flatMapLatest { slot ->
                    _state.update { it.copy(slotIndex = slot) }
                    commitmentRepository.observeActiveForSlot(slot)
                }
                .collect { commitment ->
                    val slot = _state.value.slotIndex
                    if (commitment == null) {
                        _state.value = FocusUiState(slotIndex = slot)
                        _raUi.value = GameRaUiState()
                        launchedSession = false
                        return@collect
                    }
                    refreshFocus(commitment)
                }
        }
    }

    private suspend fun refreshFocus(commitment: CommitmentEntity) {
        val game = libraryRepository.getGame(commitment.gameId) ?: return
        val system = libraryRepository.getSystem(game.systemId)
        val media = libraryRepository.observeMedia(game.id).first()
        _state.update {
            it.copy(
                slotIndex = commitment.slotIndex,
                commitment = commitment,
                game = game,
                system = system,
                media = media,
                playtimeMs = commitmentRepository.totalPlaytimeMs(commitment.id),
                sessionCount = commitmentRepository.sessionCount(commitment.id),
            )
        }
        refreshRaStatus(game, system)
        if (!_state.value.extrasLoaded) loadExtras()
    }

    fun refreshStats() {
        viewModelScope.launch {
            val c = _state.value.commitment ?: return@launch
            _state.update {
                it.copy(
                    playtimeMs = commitmentRepository.totalPlaytimeMs(c.id),
                    sessionCount = commitmentRepository.sessionCount(c.id),
                )
            }
        }
    }

    fun loadExtras() {
        viewModelScope.launch {
            val game = _state.value.game ?: return@launch
            val settings = settingsRepository.current()
            _state.update {
                it.copy(
                    loadingExtras = true,
                    hltbEnabled = settings.hltbEnabled,
                    hltbPhase = if (settings.hltbEnabled) HltbUiPhase.Loading else HltbUiPhase.Missing,
                )
            }
            if (!settings.hltbEnabled) {
                _state.update {
                    it.copy(
                        hltb = null,
                        hltbEnabled = false,
                        hltbPhase = HltbUiPhase.Missing,
                        loadingExtras = false,
                        extrasLoaded = true,
                    )
                }
                return@launch
            }
            when (val result = hltbClient.search(game.title)) {
                is HltbLookupResult.Found -> {
                    _state.update {
                        it.copy(
                            hltb = result.estimate,
                            hltbEnabled = true,
                            hltbPhase = HltbUiPhase.Ready,
                            loadingExtras = false,
                            extrasLoaded = true,
                        )
                    }
                }
                HltbLookupResult.NotFound -> {
                    _state.update {
                        it.copy(
                            hltb = null,
                            hltbEnabled = true,
                            hltbPhase = HltbUiPhase.Missing,
                            loadingExtras = false,
                            extrasLoaded = true,
                        )
                    }
                }
                is HltbLookupResult.Failed -> {
                    _state.update {
                        it.copy(
                            hltb = null,
                            hltbEnabled = true,
                            hltbPhase = HltbUiPhase.Error,
                            loadingExtras = false,
                            extrasLoaded = true,
                        )
                    }
                }
            }
        }
    }

    private fun refreshRaStatus(game: GameEntity?, system: SystemEntity?) {
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
                systemFolder = system?.folderName,
                knownGameId = game.raGameId,
                romPathsJson = game.romPathsJson,
            )
            when (lookupResult) {
                is RaResult.Ok -> libraryRepository.saveRaGameId(game.id, lookupResult.value.gameId)
                is RaResult.Unsupported -> libraryRepository.clearRaGameId(game.id)
                is RaResult.Failed -> { /* keep cached id on transient errors */ }
            }

            val config = libraryRepository.observeGameConfig(game.id).first()
            val effectiveEmulator = if (config?.useOverride == true && !config.emulatorKey.isNullOrBlank()) {
                config.emulatorKey
            } else {
                system?.defaultEmulatorKey.orEmpty()
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
                            systemFolder = system?.folderName,
                            romsDirPath = settings.romsDirPath,
                            romsTreeUri = settings.romsDirUri,
                        ),
                    ),
                ),
            )
        }
    }

    fun play(onLaunched: () -> Unit) {
        viewModelScope.launch {
            val commitment = _state.value.commitment ?: return@launch
            val game = _state.value.game ?: return@launch
            commitmentRepository.startSession(commitment.id)
            launchedSession = true
            val err = launchResolver.launch(game.id)
            if (err != null) {
                commitmentRepository.endOpenSession(commitment.id)
                launchedSession = false
                _state.update { it.copy(launchError = err) }
            } else {
                _state.update { it.copy(launchError = null) }
                onLaunched()
            }
        }
    }

    fun onReturnFromEmulator() {
        viewModelScope.launch {
            playStatsTracker.onAppForeground()
            val commitment = _state.value.commitment ?: return@launch
            if (launchedSession) {
                commitmentRepository.endOpenSession(commitment.id)
                launchedSession = false
                refreshStats()
                val game = _state.value.game
                val system = _state.value.system
                if (game != null) {
                    _state.update {
                        it.copy(game = libraryRepository.getGame(game.id) ?: game)
                    }
                }
                refreshRaStatus(_state.value.game, system)
            }
        }
    }

    fun finish(stars: Float, review: String?, onComplete: () -> Unit) {
        release(CommitmentStatus.FINISHED, stars, review, onComplete)
    }

    fun drop(stars: Float, review: String?, onComplete: () -> Unit) {
        release(CommitmentStatus.DROPPED, stars, review, onComplete)
    }

    private fun release(
        status: CommitmentStatus,
        stars: Float,
        review: String?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            val s = _state.value
            val commitment = s.commitment ?: return@launch
            val game = s.game ?: return@launch
            val box = s.media.firstOrNull {
                it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D
            }?.path
            val hours = s.playtimeMs / 3_600_000.0
            val collagePath = runCardStore.createRunCard(
                commitmentId = commitment.id,
                systemFolder = s.system?.folderName.orEmpty(),
                fileName = game.fileName,
                committedAt = commitment.committedAt,
                input = CollageInput(
                    title = game.title,
                    systemName = s.system?.displayName ?: "",
                    boxArtPath = box,
                    stars = stars,
                    reviewExcerpt = review?.take(120),
                    playtimeHours = hours,
                    sessionCount = s.sessionCount,
                    statusLabel = if (status == CommitmentStatus.FINISHED) "Finished" else "Dropped",
                    raEarned = null,
                    raTotal = null,
                ),
            )
            playCompletionStore.lastCompletion = PlayCompletionData(
                collagePath = collagePath,
                gameTitle = game.title,
                systemName = s.system?.displayName.orEmpty(),
                status = status,
                stars = stars,
                playtimeMs = s.playtimeMs,
                sessionCount = s.sessionCount,
                reviewExcerpt = review?.take(120),
            )
            val result = when (status) {
                CommitmentStatus.FINISHED -> commitmentRepository.finish(
                    commitment.id,
                    stars,
                    review,
                    collagePath,
                )
                CommitmentStatus.DROPPED -> commitmentRepository.drop(
                    commitment.id,
                    stars,
                    review,
                    collagePath,
                )
                else -> Result.failure(IllegalStateException())
            }
            result.onSuccess { onComplete() }
        }
    }
}
