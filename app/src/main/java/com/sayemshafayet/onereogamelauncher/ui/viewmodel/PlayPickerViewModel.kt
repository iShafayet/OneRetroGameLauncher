package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.ui.util.combinedLaunchCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlayPickerPhase {
    INTRO,
    PICKING,
}

data class PlayGamePick(
    val game: GameEntity,
    val systemDisplayName: String,
)

@HiltViewModel
class PlayPickerViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _phase = MutableStateFlow(PlayPickerPhase.INTRO)
    private val _suggestions = MutableStateFlow<List<PlayGamePick>>(emptyList())
    private val _wildCard = MutableStateFlow<PlayGamePick?>(null)
    private val _isLoadingPicker = MutableStateFlow(false)

    val phase: StateFlow<PlayPickerPhase> = _phase.asStateFlow()
    val isLoadingPicker: StateFlow<Boolean> = _isLoadingPicker.asStateFlow()
    val suggestions: StateFlow<List<PlayGamePick>> = _suggestions.asStateFlow()
    val wildCard: StateFlow<PlayGamePick?> = _wildCard.asStateFlow()
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val systemsById: StateFlow<Map<Long, SystemEntity>> = libraryRepository.systems
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val shelf: StateFlow<List<PlayGamePick>> = combine(
        libraryRepository.observeShelf(),
        systemsById,
    ) { games, systems ->
        games.map { game -> toPick(game, systems) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchResults: StateFlow<List<PlayGamePick>> = combine(
        query,
        libraryRepository.observeSearch(null, ""),
        systemsById,
    ) { q, all, systems ->
        val trimmed = q.trim()
        if (trimmed.length < 2) {
            emptyList()
        } else {
            all.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.fileName.contains(trimmed, ignoreCase = true) ||
                    it.developer?.contains(trimmed, ignoreCase = true) == true
            }.take(12).map { game -> toPick(game, systems) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val libraryEmpty: StateFlow<Boolean> = libraryRepository.observeSearch(null, "")
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun enterPicker() {
        if (_isLoadingPicker.value) return
        viewModelScope.launch {
            _isLoadingPicker.value = true
            try {
                val systems = systemsById.value.ifEmpty {
                    libraryRepository.systems.first().associateBy { it.id }
                }
                val pool = candidatePool()
                _suggestions.value = pool.shuffled().take(3).map { toPick(it, systems) }
                _wildCard.value = null
                _phase.value = PlayPickerPhase.PICKING
            } finally {
                _isLoadingPicker.value = false
            }
        }
    }

    fun backToIntro() {
        _phase.value = PlayPickerPhase.INTRO
        query.value = ""
        _wildCard.value = null
    }

    fun setQuery(value: String) = query.update { value }

    fun refreshSuggestions() {
        viewModelScope.launch {
            val systems = systemsById.value.ifEmpty {
                libraryRepository.systems.first().associateBy { it.id }
            }
            val pool = candidatePool()
            _suggestions.value = pool.shuffled().take(3).map { toPick(it, systems) }
        }
    }

    fun drawWildCard() {
        viewModelScope.launch {
            val systems = systemsById.value.ifEmpty {
                libraryRepository.systems.first().associateBy { it.id }
            }
            val pool = candidatePool()
            val currentIds = _suggestions.value.map { it.game.id }.toSet()
            val pick = pool.filter { it.id !in currentIds }.randomOrNull()
                ?: pool.randomOrNull()
            _wildCard.value = pick?.let { toPick(it, systems) }
        }
    }

    fun observeMedia(gameId: Long) = libraryRepository.observeMedia(gameId)

    private fun toPick(game: GameEntity, systems: Map<Long, SystemEntity>): PlayGamePick =
        PlayGamePick(
            game = game,
            systemDisplayName = systems[game.systemId]?.displayName ?: "Unknown system",
        )

    private suspend fun candidatePool(): List<GameEntity> {
        val all = libraryRepository.observeSearch(null, "").first()
        if (all.isEmpty()) return emptyList()

        val shelfIds = libraryRepository.observeShelf().first().map { it.id }.toSet()
        val onShelf = all.filter { it.id in shelfIds || it.onShelf }
        val favorites = all.filter { it.favorite && it.id !in shelfIds }
        val neverStarted = all.filter {
            it.completedStatus == null &&
                it.combinedLaunchCount() == 0 &&
                it.id !in shelfIds &&
                !it.favorite
        }
        val replayable = all.filter {
            it.id !in shelfIds &&
                !it.favorite &&
                (it.completedStatus != null || it.combinedLaunchCount() > 0)
        }
        return (onShelf + favorites + neverStarted + replayable)
            .distinctBy { it.id }
            .ifEmpty { all }
    }
}
