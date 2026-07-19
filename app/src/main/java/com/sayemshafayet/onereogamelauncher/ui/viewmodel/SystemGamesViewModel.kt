package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.SystemEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
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

enum class GameListFilter { ALL, FAVORITE, UNPLAYED, FINISHED, DROPPED }

@HiltViewModel
class SystemGamesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    val systemId: Long = savedStateHandle.get<String>("systemId")?.toLongOrNull() ?: 0L

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(GameListFilter.ALL)

    val system: StateFlow<SystemEntity?> = libraryRepository.systems.map { systems ->
        systems.firstOrNull { it.id == systemId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val games: StateFlow<List<GameEntity>> = combine(
        libraryRepository.observeGamesBySystem(systemId),
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
                GameListFilter.UNPLAYED -> game.completedStatus == null && game.playcount == 0
                GameListFilter.FINISHED -> game.completedStatus == com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus.FINISHED
                GameListFilter.DROPPED -> game.completedStatus == com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus.DROPPED
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchQuery: StateFlow<String> = query.asStateFlow()
    val gameFilter: StateFlow<GameListFilter> = filter.asStateFlow()

    fun setQuery(value: String) = query.update { value }
    fun setFilter(value: GameListFilter) = filter.update { value }

    fun observeMedia(gameId: Long) = libraryRepository.observeMedia(gameId)
}
