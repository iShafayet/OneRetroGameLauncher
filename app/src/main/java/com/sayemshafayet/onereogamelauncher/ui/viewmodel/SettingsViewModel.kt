package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SettingsHubUi())
    val ui: StateFlow<SettingsHubUi> = _ui.asStateFlow()

    val scanProgress: StateFlow<ScanProgress?> = libraryRepository.scanProgress

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
            rescan()
        }
    }

    fun onOrglDataFolderPicked(uri: Uri) {
        viewModelScope.launch {
            takePersistable(uri, write = true)
            val pathHint = SafPathResolver.resolvePath(context, uri)
            settingsRepository.setOrglDataDir(uri.toString(), pathHint)
            _ui.update {
                it.copy(
                    orglDataPath = pathHint.orEmpty(),
                    orglDataUri = uri.toString(),
                    orglDataDisplay = SafPathResolver.displayLabel(uri.toString(), pathHint),
                )
            }
        }
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
                )
            }
            // Re-resolve media/metadata from ES-DE without rewriting anything.
            if (_ui.value.romsUri.isNotBlank() || _ui.value.romsPath.isNotBlank()) {
                rescan()
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

    fun rescan() {
        viewModelScope.launch {
            _ui.update { it.copy(scanning = true, scanMessage = null) }
            runCatching {
                libraryRepository.ensureCatalogLoaded()
                libraryRepository.scanLibrary()
            }.onSuccess { result ->
                _ui.update {
                    it.copy(
                        scanning = false,
                        scanMessage = when {
                            result.gamesFound > 0 ->
                                "Found ${result.gamesFound} games across ${result.systemsScanned} systems" +
                                    (if (result.mediaLinked > 0) ", linked ${result.mediaLinked} media files"
                                    else ", 0 media linked — check ES-DE data folder contains downloaded_media/")
                            result.systemsScanned == 0 ->
                                "No system folders found under the selected directory. " +
                                    "Pick the ROMs root that contains nes/, snes/, psx/, …"
                            else ->
                                "Scanned ${result.systemsScanned} systems but found 0 matching ROM files."
                        },
                    )
                }
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        scanning = false,
                        scanMessage = e.message ?: "Scan failed",
                    )
                }
            }
        }
    }
}
