package com.sayemshafayet.onereogamelauncher.ui.viewmodel

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
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.RaProgress
import com.sayemshafayet.onereogamelauncher.hltb.HowLongToBeatClient
import com.sayemshafayet.onereogamelauncher.launch.LaunchResolver
import com.sayemshafayet.onereogamelauncher.play.PlayStatsTracker
import com.sayemshafayet.onereogamelauncher.play.CollageGenerator
import com.sayemshafayet.onereogamelauncher.play.CollageInput
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val commitment: CommitmentEntity? = null,
    val game: GameEntity? = null,
    val system: SystemEntity? = null,
    val media: List<MediaEntity> = emptyList(),
    val playtimeMs: Long = 0,
    val sessionCount: Int = 0,
    val hltb: HltbEstimate? = null,
    val ra: RaProgress? = null,
    val launchError: String? = null,
    val loadingExtras: Boolean = false,
    val extrasLoaded: Boolean = false,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val commitmentRepository: CommitmentRepository,
    private val libraryRepository: LibraryRepository,
    private val launchResolver: LaunchResolver,
    private val settingsRepository: SettingsRepository,
    private val hltbClient: HowLongToBeatClient,
    private val raClient: RetroAchievementsClient,
    private val collageGenerator: CollageGenerator,
    private val playStatsTracker: PlayStatsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    val activeCommitment = commitmentRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var launchedSession = false

    init {
        viewModelScope.launch {
            commitmentRepository.observeActive().collect { commitment ->
                if (commitment == null) {
                    _state.value = FocusUiState()
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
                commitment = commitment,
                game = game,
                system = system,
                media = media,
                playtimeMs = commitmentRepository.totalPlaytimeMs(commitment.id),
                sessionCount = commitmentRepository.sessionCount(commitment.id),
            )
        }
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
            _state.update { it.copy(loadingExtras = true) }
            val hltb = if (settings.hltbEnabled) hltbClient.search(game.title) else null
            val system = _state.value.system
            val ra = if (settings.retroAchievementsConfigured()) {
                raClient.fetchProgress(
                    settings = settings,
                    romPath = game.romPath,
                    title = game.title,
                    systemFolder = system?.folderName,
                    knownGameId = game.raGameId,
                    romPathsJson = game.romPathsJson,
                )
            } else {
                null
            }
            _state.update { it.copy(hltb = hltb, ra = ra, loadingExtras = false, extrasLoaded = true) }
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
            }
        }
    }

    fun finish(stars: Float, review: String?, onCollage: (String?) -> Unit) {
        release(CommitmentStatus.FINISHED, stars, review, onCollage)
    }

    fun drop(stars: Float, review: String?, onCollage: (String?) -> Unit) {
        release(CommitmentStatus.DROPPED, stars, review, onCollage)
    }

    private fun release(
        status: CommitmentStatus,
        stars: Float,
        review: String?,
        onCollage: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val s = _state.value
            val commitment = s.commitment ?: return@launch
            val game = s.game ?: return@launch
            val box = s.media.firstOrNull {
                it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D
            }?.path
            val hours = s.playtimeMs / 3_600_000.0
            val collagePath = collageGenerator.generate(
                CollageInput(
                    title = game.title,
                    systemName = s.system?.displayName ?: "",
                    boxArtPath = box,
                    stars = stars,
                    reviewExcerpt = review?.take(120),
                    playtimeHours = hours,
                    sessionCount = s.sessionCount,
                    statusLabel = if (status == CommitmentStatus.FINISHED) "Finished" else "Dropped",
                    raEarned = s.ra?.earned,
                    raTotal = s.ra?.total,
                ),
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
            result.onSuccess { onCollage(collagePath) }
        }
    }
}
