package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
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

enum class PlaySuggestionBadge {
    WISHLIST,
    SUGGESTED,
}

data class PlayGamePick(
    val game: GameEntity,
    val systemDisplayName: String,
    val badge: PlaySuggestionBadge? = null,
)

@HiltViewModel
class PlayPickerViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val commitmentRepository: CommitmentRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _phase = MutableStateFlow(PlayPickerPhase.INTRO)
    private val _suggestions = MutableStateFlow<List<PlayGamePick>>(emptyList())
    private val _isLoadingPicker = MutableStateFlow(false)

    val phase: StateFlow<PlayPickerPhase> = _phase.asStateFlow()
    val isLoadingPicker: StateFlow<Boolean> = _isLoadingPicker.asStateFlow()
    val suggestions: StateFlow<List<PlayGamePick>> = _suggestions.asStateFlow()
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
                refreshSuggestionsInternal()
                _phase.value = PlayPickerPhase.PICKING
            } finally {
                _isLoadingPicker.value = false
            }
        }
    }

    fun backToIntro() {
        _phase.value = PlayPickerPhase.INTRO
        query.value = ""
    }

    fun setQuery(value: String) = query.update { value }

    fun refreshSuggestions() {
        viewModelScope.launch { refreshSuggestionsInternal() }
    }

    fun observeMedia(gameId: Long) = libraryRepository.observeMedia(gameId)

    private suspend fun refreshSuggestionsInternal() {
        val systems = systemsById.value.ifEmpty {
            libraryRepository.systems.first().associateBy { it.id }
        }
        val available = availableGames()
        _suggestions.value = buildTonightTrio(available).map { (game, badge) ->
            toPick(game, systems, badge)
        }
    }

    private fun toPick(
        game: GameEntity,
        systems: Map<Long, SystemEntity>,
        badge: PlaySuggestionBadge? = null,
    ): PlayGamePick =
        PlayGamePick(
            game = game,
            systemDisplayName = systems[game.systemId]?.displayName ?: "Unknown system",
            badge = badge,
        )

    private suspend fun availableGames(): List<GameEntity> {
        val all = libraryRepository.observeSearch(null, "").first()
        if (all.isEmpty()) return emptyList()
        val committedIds = commitmentRepository.getAllActive().map { it.gameId }.toSet()
        return all.filter { it.id !in committedIds }.ifEmpty { all }
    }

    /**
     * Three distinct picks:
     * 1) Wishlist if any exist, else random Suggested
     * 2) Wishlist if wishlist size > 10, else Suggested
     * 3) Always Suggested (random from remaining)
     */
    internal fun buildTonightTrio(available: List<GameEntity>): List<Pair<GameEntity, PlaySuggestionBadge>> {
        if (available.isEmpty()) return emptyList()
        val wishlist = available.filter { it.wishlisted }.shuffled()
        val used = linkedSetOf<Long>()
        val picks = mutableListOf<Pair<GameEntity, PlaySuggestionBadge>>()

        fun takeWishlist(): Boolean {
            val next = wishlist.firstOrNull { it.id !in used } ?: return false
            used += next.id
            picks += next to PlaySuggestionBadge.WISHLIST
            return true
        }

        fun takeSuggested(): Boolean {
            val next = available.filter { it.id !in used }.randomOrNull() ?: return false
            used += next.id
            picks += next to PlaySuggestionBadge.SUGGESTED
            return true
        }

        if (wishlist.isNotEmpty()) takeWishlist() else takeSuggested()
        if (wishlist.size > 10) {
            if (!takeWishlist()) takeSuggested()
        } else {
            takeSuggested()
        }
        takeSuggested()
        return picks
    }
}
