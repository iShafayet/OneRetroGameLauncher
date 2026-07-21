package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionData
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionStore
import com.sayemshafayet.onereogamelauncher.play.toPlayCompletionData
import com.sayemshafayet.onereogamelauncher.ui.util.GallerySaver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlayCompletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val completionStore: PlayCompletionStore,
    private val commitmentRepository: CommitmentRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val historyCommitmentId: Long? =
        savedStateHandle.get<String>("commitmentId")?.toLongOrNull()

    val isHistoryView: Boolean = historyCommitmentId != null

    private val _completion = MutableStateFlow<PlayCompletionData?>(null)
    val completion: StateFlow<PlayCompletionData?> = _completion.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savedToGallery = MutableStateFlow(false)
    val savedToGallery: StateFlow<Boolean> = _savedToGallery.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _completion.value = when (val id = historyCommitmentId) {
                null -> completionStore.lastCompletion
                else -> commitmentRepository.getJournalEntry(id)?.toPlayCompletionData()
            }
            _isLoading.value = false
        }
    }

    fun saveToGallery() {
        val data = _completion.value ?: return
        val path = data.collagePath
        if (path.isNullOrBlank()) {
            _saveMessage.value = "No run card was generated"
            return
        }
        if (_isSaving.value || _savedToGallery.value) return
        viewModelScope.launch {
            _isSaving.value = true
            _saveMessage.value = null
            GallerySaver.savePng(
                context = context,
                sourceFile = File(path),
                displayName = "ORGL_${data.gameTitle}",
            ).onSuccess {
                _savedToGallery.value = true
                _saveMessage.value = "Saved to Pictures/ORGL"
            }.onFailure {
                _saveMessage.value = it.message ?: "Could not save to gallery"
            }
            _isSaving.value = false
        }
    }

    fun clearCompletion() {
        if (!isHistoryView) {
            completionStore.lastCompletion = null
        }
    }
}
