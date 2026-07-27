package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.legal.LegalDocuments
import com.sayemshafayet.onereogamelauncher.launch.RetroArchLauncher
import com.sayemshafayet.onereogamelauncher.play.CommitmentRepository
import com.sayemshafayet.onereogamelauncher.ui.util.SafFolderAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    val activeCommitments = commitmentRepository.observeAllActive().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    init {
        viewModelScope.launch {
            libraryRepository.ensureCatalogLoaded()
            var settings = settingsRepository.settings.first()
            if (settings.preferredRetroArchPackage.isBlank()) {
                settingsRepository.setPreferredRetroArchPackage(RetroArchLauncher.DEFAULT_PACKAGE)
                settings = settingsRepository.settings.first()
            }
            reconcileSafAccess(settings)
            _startupReady.value = true
        }
    }

    fun checkSafAccessOnResume() {
        viewModelScope.launch {
            reconcileSafAccess(settingsRepository.settings.first())
        }
    }

    fun ensureCatalog() {
        viewModelScope.launch { libraryRepository.ensureCatalogLoaded() }
    }

    private suspend fun reconcileSafAccess(settings: AppSettings) {
        val action = runCatching { SafFolderAccess.validate(context, settings) }
            .onFailure { Log.w(TAG, "SAF access check failed", it) }
            .getOrDefault(SafFolderAccess.Action.None)
        when (action) {
            SafFolderAccess.Action.None -> Unit
            SafFolderAccess.Action.ResetForReOnboarding -> {
                Log.i(TAG, "Required SAF folder access lost — resetting to onboarding")
                settingsRepository.resetForLostSafAccess()
            }
            SafFolderAccess.Action.ClearEsdeOnly -> {
                Log.i(TAG, "ES-DE SAF folder access lost — clearing optional ES-DE folder")
                runCatching { libraryRepository.purgeEsdeLinkedMedia() }
                    .onFailure { Log.w(TAG, "Failed to purge ES-DE media rows", it) }
                settingsRepository.clearEsdeDataDir()
            }
        }
    }

    fun acceptLegalDocuments(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            settingsRepository.setLegalAcceptedVersion(LegalDocuments.VERSION)
            onDone()
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
