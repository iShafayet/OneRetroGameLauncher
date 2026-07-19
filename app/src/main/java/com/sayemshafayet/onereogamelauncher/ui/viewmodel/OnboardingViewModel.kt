package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OnboardingUiState(
    val page: Int = 0,
    val romsUri: String? = null,
    val romsPath: String? = null,
    val orglUri: String? = null,
    val orglPath: String? = null,
    val orglReused: Boolean = false,
    val orglError: String? = null,
    val orglIncompatibleAlert: String? = null,
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

    fun nextPage() = _state.update { it.copy(page = (it.page + 1).coerceAtMost(4)) }
    fun prevPage() = _state.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }

    fun dismissOrglIncompatibleAlert() {
        _state.update { it.copy(orglIncompatibleAlert = null) }
    }

    fun onRomsFolderPicked(uri: Uri) {
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

    fun onOrglFolderPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update {
                it.copy(orglError = null, orglIncompatibleAlert = null)
            }
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            val result = withContext(Dispatchers.IO) {
                OrglDataDirectory.prepare(context, uri)
            }
            when (result) {
                is OrglDataDirectory.PrepareResult.Ready -> {
                    val pathHint = SafPathResolver.resolvePath(context, uri)
                    _state.update {
                        it.copy(
                            orglUri = uri.toString(),
                            orglPath = pathHint,
                            orglReused = result.reusedExisting,
                            orglError = null,
                            orglIncompatibleAlert = null,
                        )
                    }
                }
                is OrglDataDirectory.PrepareResult.Incompatible -> {
                    _state.update {
                        it.copy(
                            orglUri = null,
                            orglPath = null,
                            orglReused = false,
                            orglError = null,
                            orglIncompatibleAlert = OrglDataDirectory.incompatibleMessage(
                                result.foundVersion,
                            ),
                        )
                    }
                }
                is OrglDataDirectory.PrepareResult.Failed -> {
                    _state.update {
                        it.copy(
                            orglUri = null,
                            orglPath = null,
                            orglReused = false,
                            orglError = result.message,
                            orglIncompatibleAlert = null,
                        )
                    }
                }
            }
        }
    }

    fun finishOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            val romsUri = current.romsUri ?: return@launch
            val orglUri = current.orglUri ?: return@launch
            settingsRepository.setRomsDir(romsUri, current.romsPath)
            settingsRepository.setOrglDataDir(orglUri, current.orglPath)
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
                                ?: "Scan failed — open Settings → Folders and rescan.",
                            scanDone = true,
                        )
                    }
                    onComplete()
                }
        }
    }
}
