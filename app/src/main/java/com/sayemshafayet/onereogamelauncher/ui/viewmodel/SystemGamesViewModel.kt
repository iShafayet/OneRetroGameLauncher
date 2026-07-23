package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.VirtualLibrarySystem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameListFilter { ALL, FAVORITE, FINISHED, DROPPED }

@HiltViewModel
class SystemGamesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val systemId: Long = savedStateHandle.get<String>("systemId")?.toLongOrNull() ?: 0L

    private val virtualSystem = VirtualLibrarySystem.fromId(systemId)

    val isVirtualSystem: Boolean = virtualSystem != null

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(GameListFilter.ALL)

    val system: StateFlow<SystemEntity?> = libraryRepository.systems
        .map { systems ->
            if (virtualSystem != null) null else systems.firstOrNull { it.id == systemId }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val screenTitle: StateFlow<String> = if (virtualSystem != null) {
        MutableStateFlow(virtualSystem.displayName).asStateFlow()
    } else {
        system.map { it?.displayName ?: "Games" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Games")
    }

    private val sourceGames: StateFlow<List<GameEntity>> = when (virtualSystem) {
        VirtualLibrarySystem.FAVORITES -> libraryRepository.observeFavorites()
        VirtualLibrarySystem.RECENT -> libraryRepository.observeRecent()
        null -> libraryRepository.observeGamesBySystem(systemId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val games: StateFlow<List<GameEntity>> = combine(
        sourceGames,
        query,
        filter,
    ) { allGames, q, f ->
        allGames.filter { game ->
            val matchesQuery = q.isBlank() ||
                game.title.contains(q, ignoreCase = true) ||
                game.fileName.contains(q, ignoreCase = true)
            val matchesFilter = when (f) {
                GameListFilter.ALL -> true
                GameListFilter.FAVORITE -> game.favorite
                GameListFilter.FINISHED -> game.completedStatus == GameCompletedStatus.FINISHED
                GameListFilter.DROPPED -> game.completedStatus == GameCompletedStatus.DROPPED
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchQuery: StateFlow<String> = query.asStateFlow()
    val gameFilter: StateFlow<GameListFilter> = filter.asStateFlow()

    val layout: StateFlow<GameListLayout> = settingsRepository.settings
        .map { it.gameListLayout }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameListLayout.GRID)

    fun setQuery(value: String) = query.update { value }
    fun setFilter(value: GameListFilter) = filter.update { value }

    fun toggleLayout() {
        viewModelScope.launch {
            val next = if (layout.value == GameListLayout.GRID) {
                GameListLayout.LIST
            } else {
                GameListLayout.GRID
            }
            settingsRepository.setGameListLayout(next)
        }
    }

    fun observeMedia(gameId: Long) = libraryRepository.observeMedia(gameId)
}
