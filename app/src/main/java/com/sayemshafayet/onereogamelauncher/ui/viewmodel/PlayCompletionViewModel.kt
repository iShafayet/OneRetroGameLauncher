package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionData
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionStore
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
    private val completionStore: PlayCompletionStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val completion: PlayCompletionData?
        get() = completionStore.lastCompletion

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savedToGallery = MutableStateFlow(false)
    val savedToGallery: StateFlow<Boolean> = _savedToGallery.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun saveToGallery() {
        val data = completion ?: return
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
        completionStore.lastCompletion = null
    }
}
