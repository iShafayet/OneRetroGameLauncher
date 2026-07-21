package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsHubUi(
    val romsPath: String = "",
    val romsUri: String = "",
    val romsDisplay: String = "Not set",
    val orglDataPath: String = "",
    val orglDataUri: String = "",
    val orglDataDisplay: String = "Not set",
    val esdeDataPath: String = "",
    val esdeDataUri: String = "",
    val esdeDataDisplay: String = "Not set",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val retroArchPkg: String = "",
    val ssConfigured: Boolean = false,
    val raConfigured: Boolean = false,
    val hltbEnabled: Boolean = true,
    val scanning: Boolean = false,
    val scanMessage: String? = null,
    val orglIncompatibleAlert: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SettingsHubUi())
    val ui: StateFlow<SettingsHubUi> = _ui.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                _ui.update {
                    it.copy(
                        romsPath = s.romsDirPath.orEmpty(),
                        romsUri = s.romsDirUri.orEmpty(),
                        romsDisplay = SafPathResolver.displayLabel(s.romsDirUri, s.romsDirPath),
                        orglDataPath = s.orglDataDirPath.orEmpty(),
                        orglDataUri = s.orglDataDirUri.orEmpty(),
                        orglDataDisplay = SafPathResolver.displayLabel(s.orglDataDirUri, s.orglDataDirPath),
                        esdeDataPath = s.esdeDataDirPath.orEmpty(),
                        esdeDataUri = s.esdeDataDirUri.orEmpty(),
                        esdeDataDisplay = SafPathResolver.displayLabel(s.esdeDataDirUri, s.esdeDataDirPath),
                        themeMode = s.themeMode,
                        retroArchPkg = s.preferredRetroArchPackage,
                        ssConfigured = s.screenScraperUser.isNotBlank(),
                        raConfigured = s.retroAchievementsUser.isNotBlank() &&
                            s.retroAchievementsPassword.isNotBlank(),
                        hltbEnabled = s.hltbEnabled,
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setRetroArchPackage(pkg: String) {
        viewModelScope.launch { settingsRepository.setPreferredRetroArchPackage(pkg.trim()) }
    }

    fun onRomsFolderPicked(uri: Uri) {
        viewModelScope.launch {
            takePersistable(uri, write = false)
            val pathHint = SafPathResolver.resolvePath(context, uri)
            settingsRepository.setRomsDir(uri.toString(), pathHint)
            _ui.update {
                it.copy(
                    romsPath = pathHint.orEmpty(),
                    romsUri = uri.toString(),
                    romsDisplay = SafPathResolver.displayLabel(uri.toString(), pathHint),
                    scanMessage = null,
                )
            }
        }
    }

    fun onOrglDataFolderPicked(uri: Uri) {
        viewModelScope.launch {
            takePersistable(uri, write = true)
            val result = withContext(Dispatchers.IO) {
                OrglDataDirectory.prepare(context, uri)
            }
            when (result) {
                is OrglDataDirectory.PrepareResult.Ready -> {
                    val pathHint = SafPathResolver.resolvePath(context, uri)
                    settingsRepository.setOrglDataDir(uri.toString(), pathHint)
                    _ui.update {
                        it.copy(
                            orglDataPath = pathHint.orEmpty(),
                            orglDataUri = uri.toString(),
                            orglDataDisplay = SafPathResolver.displayLabel(uri.toString(), pathHint),
                            scanMessage = if (result.reusedExisting) {
                                "Linked existing ORGL data folder (spec v${OrglDataDirectory.SPEC_VERSION})."
                            } else {
                                "ORGL data folder ready (${OrglDataDirectory.META_FILE_NAME} created)."
                            },
                            orglIncompatibleAlert = null,
                        )
                    }
                }
                is OrglDataDirectory.PrepareResult.Incompatible -> {
                    _ui.update {
                        it.copy(
                            orglIncompatibleAlert = OrglDataDirectory.incompatibleMessage(
                                result.foundVersion,
                            ),
                            scanMessage = null,
                        )
                    }
                }
                is OrglDataDirectory.PrepareResult.Failed -> {
                    _ui.update {
                        it.copy(
                            scanMessage = result.message,
                            orglIncompatibleAlert = null,
                        )
                    }
                }
            }
        }
    }

    fun dismissOrglIncompatibleAlert() {
        _ui.update { it.copy(orglIncompatibleAlert = null) }
    }

    fun onEsdeDataFolderPicked(uri: Uri) {
        viewModelScope.launch {
            takePersistable(uri, write = false)
            val pathHint = SafPathResolver.resolvePath(context, uri)
            settingsRepository.setEsdeDataDir(uri.toString(), pathHint)
            _ui.update {
                it.copy(
                    esdeDataPath = pathHint.orEmpty(),
                    esdeDataUri = uri.toString(),
                    esdeDataDisplay = SafPathResolver.displayLabel(uri.toString(), pathHint),
                    scanMessage = null,
                )
            }
        }
    }

    fun unlinkEsde() {
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, scanMessage = null) }
            runCatching {
                val removed = libraryRepository.purgeEsdeLinkedMedia()
                settingsRepository.clearEsdeDataDir()
                removed
            }.onSuccess { removed ->
                _ui.update {
                    it.copy(
                        scanning = false,
                        esdeDataPath = "",
                        esdeDataUri = "",
                        esdeDataDisplay = "Not set",
                        scanMessage = "ES-DE unlinked. Removed $removed cached media entries.",
                    )
                }
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        scanning = false,
                        scanMessage = e.message ?: "Could not unlink ES-DE",
                    )
                }
            }
        }
    }

    private fun takePersistable(uri: Uri, write: Boolean) {
        var takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (write) takeFlags = takeFlags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
    }
}
