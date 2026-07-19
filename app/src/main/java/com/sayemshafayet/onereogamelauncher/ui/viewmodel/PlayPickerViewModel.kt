package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayPickerViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val shelf: StateFlow<List<GameEntity>> = libraryRepository.observeShelf()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchResults: StateFlow<List<GameEntity>> = combine(
        query,
        libraryRepository.observeSearch(null, ""),
    ) { q, all ->
        val trimmed = q.trim()
        if (trimmed.length < 2) emptyList()
        else all.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.fileName.contains(trimmed, ignoreCase = true)
        }.take(12)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _finalists = MutableStateFlow<List<GameEntity>>(emptyList())
    val finalists: StateFlow<List<GameEntity>> = _finalists.asStateFlow()

    val searchQuery: StateFlow<String> = query.asStateFlow()

    fun setQuery(value: String) = query.update { value }

    fun surpriseMe(onResult: (GameEntity?) -> Unit) {
        viewModelScope.launch {
            val pool = unplayedGames()
            onResult(pool.randomOrNull())
        }
    }

    fun pickFinalists() {
        viewModelScope.launch {
            val pool = unplayedGames()
            _finalists.value = pool.shuffled().take(3)
        }
    }

    fun observeMedia(gameId: Long) = libraryRepository.observeMedia(gameId)

    private suspend fun unplayedGames(): List<GameEntity> {
        val all = libraryRepository.observeSearch(null, "").first()
        return all.filter { it.completedStatus == null && it.playcount == 0 }
    }
}
