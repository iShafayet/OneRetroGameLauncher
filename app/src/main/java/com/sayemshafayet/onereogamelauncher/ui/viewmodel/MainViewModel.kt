package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val commitmentRepository: CommitmentRepository,
) : ViewModel() {
    private val _startupReady = MutableStateFlow(false)
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings(),
    )

    val scanProgress: StateFlow<ScanProgress?> = libraryRepository.scanProgress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val activeCommitment = commitmentRepository.observeActive().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    init {
        viewModelScope.launch {
            libraryRepository.ensureCatalogLoaded()
            settingsRepository.settings.first()
            _startupReady.value = true
        }
    }

    fun ensureCatalog() {
        viewModelScope.launch { libraryRepository.ensureCatalogLoaded() }
    }
}
