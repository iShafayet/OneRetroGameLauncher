package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.prefs.retroAchievementsConfigured
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.RaButtonState
import com.sayemshafayet.onereogamelauncher.domain.RaGameDetails
import com.sayemshafayet.onereogamelauncher.domain.RaResult
import com.sayemshafayet.onereogamelauncher.domain.RaVisualState
import com.sayemshafayet.onereogamelauncher.ra.RaSupportEvaluator
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
import com.sayemshafayet.onereogamelauncher.ra.RomHashCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RaScreenOutcome {
    /** Transient failure — network, auth, API error. User can retry. */
    ERROR,
    /** Game or ROM is not on RetroAchievements. */
    NOT_SUPPORTED,
}

data class GameRaUiState(
    val loading: Boolean = true,
    val button: RaButtonState = RaButtonState(
        visual = RaVisualState.NO_GAME,
        subtitle = "",
        enabled = false,
    ),
)

data class GameRetroAchievementsUiState(
    val loading: Boolean = true,
    val outcome: RaScreenOutcome? = null,
    val message: String? = null,
    val details: RaGameDetails? = null,
    val button: RaButtonState? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class GameRetroAchievementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val raClient: RetroAchievementsClient,
    private val raSupportEvaluator: RaSupportEvaluator,
    private val romHashCalculator: RomHashCalculator,
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<String>("gameId")?.toLongOrNull() ?: 0L

    private val _ui = MutableStateFlow(GameRetroAchievementsUiState())
    val ui: StateFlow<GameRetroAchievementsUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, outcome = null, message = null) }
            val game = libraryRepository.getGame(gameId)
            if (game == null) {
                _ui.update {
                    it.copy(
                        loading = false,
                        outcome = RaScreenOutcome.ERROR,
                        message = "Game not found",
                    )
                }
                return@launch
            }
            val settings = settingsRepository.settings.first()
            val signedIn = settings.retroAchievementsConfigured()
            if (!signedIn) {
                _ui.update {
                    it.copy(
                        loading = false,
                        signedIn = false,
                        button = RaButtonState(
                            visual = RaVisualState.SIGN_IN_REQUIRED,
                            subtitle = "Add username and password in Settings",
                            enabled = true,
                        ),
                    )
                }
                return@launch
            }

            val config = libraryRepository.getGameConfig(gameId)
            val system = libraryRepository.getSystem(game.systemId)
            val effectiveEmulator = if (config?.useOverride == true) {
                config.emulatorKey
            } else {
                system?.defaultEmulatorKey
            }.orEmpty().ifBlank { "RETROARCH" }

            val lookupResult = raClient.lookupGame(
                settings = settings,
                romPath = game.romPath,
                title = game.title,
                systemFolder = system?.folderName,
                knownGameId = game.raGameId,
                romPathsJson = game.romPathsJson,
            )
            when (lookupResult) {
                is RaResult.Ok -> libraryRepository.saveRaGameId(gameId, lookupResult.value.gameId)
                is RaResult.Unsupported -> libraryRepository.clearRaGameId(gameId)
                is RaResult.Failed -> { /* keep cached id on transient errors */ }
            }

            val button = raSupportEvaluator.evaluateButton(
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
            )

            when (lookupResult) {
                is RaResult.Unsupported -> {
                    _ui.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            button = button,
                            outcome = RaScreenOutcome.NOT_SUPPORTED,
                            message = lookupResult.message,
                        )
                    }
                    return@launch
                }
                is RaResult.Failed -> {
                    _ui.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            button = button,
                            outcome = RaScreenOutcome.ERROR,
                            message = lookupResult.message,
                        )
                    }
                    return@launch
                }
                is RaResult.Ok -> Unit
            }

            val detailsResult = raClient.fetchGameDetails(
                settings = settings,
                romPath = game.romPath,
                title = game.title,
                systemFolder = system?.folderName,
                knownGameId = (lookupResult as RaResult.Ok).value.gameId,
                romPathsJson = game.romPathsJson,
            )

            when (detailsResult) {
                is RaResult.Ok -> {
                    _ui.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            details = detailsResult.value,
                            button = button,
                        )
                    }
                }
                is RaResult.Unsupported -> {
                    _ui.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            button = button,
                            outcome = RaScreenOutcome.NOT_SUPPORTED,
                            message = detailsResult.message,
                        )
                    }
                }
                is RaResult.Failed -> {
                    _ui.update {
                        it.copy(
                            loading = false,
                            signedIn = true,
                            button = button,
                            outcome = RaScreenOutcome.ERROR,
                            message = detailsResult.message,
                        )
                    }
                }
            }
        }
    }
}
