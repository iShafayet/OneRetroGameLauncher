package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CommitConfirmViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val commitmentRepository: CommitmentRepository,
) : ViewModel() {
    val gameId: Long = savedStateHandle.get<String>("gameId")?.toLongOrNull() ?: 0L

    val game: StateFlow<GameEntity?> = libraryRepository.observeGame(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val systemDisplayName: StateFlow<String?> = combine(
        game,
        libraryRepository.systems,
    ) { g, systems ->
        g?.let { game -> systems.firstOrNull { it.id == game.systemId }?.displayName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val media = libraryRepository.observeMedia(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isCommitting = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isCommitting: StateFlow<Boolean> = _isCommitting

    fun commit(onSuccess: () -> Unit) {
        if (_isCommitting.value) return
        viewModelScope.launch {
            _isCommitting.value = true
            _error.value = null
            commitmentRepository.commit(gameId)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
            _isCommitting.value = false
        }
    }
}
