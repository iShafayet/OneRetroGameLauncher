package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: Int = 0,
    val romsUri: String? = null,
    val romsPath: String? = null,
    val scanning: Boolean = false,
    val scanError: String? = null,
    val scanDone: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    val scanProgress: StateFlow<ScanProgress?> = libraryRepository.scanProgress

    fun nextPage() = _state.update { it.copy(page = (it.page + 1).coerceAtMost(3)) }
    fun prevPage() = _state.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }

    fun onFolderPicked(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
        val pathHint = SafPathResolver.resolvePath(context, uri)
        _state.update {
            it.copy(
                romsUri = uri.toString(),
                romsPath = pathHint,
            )
        }
    }

    fun finishOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            val uri = current.romsUri ?: return@launch
            settingsRepository.setRomsDir(uri, current.romsPath)
            settingsRepository.setOnboardingDone(true)
            _state.update { it.copy(scanning = true, scanError = null) }
            libraryRepository.ensureCatalogLoaded()
            runCatching { libraryRepository.scanLibrary() }
                .onSuccess {
                    _state.update { it.copy(scanning = false, scanDone = true) }
                    onComplete()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            scanning = false,
                            scanError = e.message
                                ?: "Scan failed — open Settings → ES-DE / Library and rescan.",
                            scanDone = true,
                        )
                    }
                    onComplete()
                }
        }
    }
}
