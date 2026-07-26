package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class LibrarySearchResult(
    val game: GameEntity,
    val systemDisplayName: String,
    val systemFolder: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibrarySearchViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val results: StateFlow<List<LibrarySearchResult>> = query
        .flatMapLatest { raw ->
            val q = raw.trim()
            if (q.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    libraryRepository.observeSearch(null, q),
                    libraryRepository.systems,
                ) { games, systems ->
                    val byId = systems.associateBy { it.id }
                    games.map { game ->
                        val system = byId[game.systemId]
                        LibrarySearchResult(
                            game = game,
                            systemDisplayName = system?.displayName.orEmpty(),
                            systemFolder = system?.folderName.orEmpty(),
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) = query.update { value }

    fun observeMedia(gameId: Long): Flow<List<MediaEntity>> =
        libraryRepository.observeMedia(gameId)
}
