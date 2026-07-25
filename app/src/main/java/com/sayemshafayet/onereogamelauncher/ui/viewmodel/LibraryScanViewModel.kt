package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.LibraryScanSummary
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.library.RomScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryScanOutcome {
    RUNNING,
    SUCCESS,
    CANCELLED,
    FAILED,
}

data class LibraryScanUi(
    val outcome: LibraryScanOutcome = LibraryScanOutcome.RUNNING,
    val message: String? = null,
    val result: RomScanResult? = null,
    val summary: LibraryScanSummary? = null,
    val esdeLinked: Boolean = false,
)

@HiltViewModel
class LibraryScanViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val scanProgress: StateFlow<ScanProgress?> = libraryRepository.scanProgress

    private val _ui = MutableStateFlow(LibraryScanUi())
    val ui: StateFlow<LibraryScanUi> = _ui.asStateFlow()

    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        com.sayemshafayet.onereogamelauncher.data.prefs.AppSettings(),
    )

    private var scanJob: Job? = null

    fun startScanIfNeeded() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            val current = settingsRepository.settings.first()
            val esdeLinked = !current.esdeDataDirUri.isNullOrBlank() ||
                !current.esdeDataDirPath.isNullOrBlank()
            val romsConfigured = !current.romsDirUri.isNullOrBlank() ||
                !current.romsDirPath.isNullOrBlank()

            if (!romsConfigured) {
                _ui.value = LibraryScanUi(
                    outcome = LibraryScanOutcome.FAILED,
                    message = "ROMs folder not configured. Set it in Settings → Folders first.",
                    esdeLinked = esdeLinked,
                )
                return@launch
            }

            _ui.value = LibraryScanUi(
                outcome = LibraryScanOutcome.RUNNING,
                esdeLinked = esdeLinked,
            )

            runCatching {
                libraryRepository.scanLibrary()
            }.onSuccess { result ->
                val summary = libraryRepository.buildLibraryScanSummary(
                    unknownFiles = result.unknownFiles.size,
                )
                _ui.value = LibraryScanUi(
                    outcome = LibraryScanOutcome.SUCCESS,
                    message = formatSuccess(result, summary, esdeLinked),
                    result = result,
                    summary = summary,
                    esdeLinked = esdeLinked,
                )
            }.onFailure { e ->
                _ui.value = when (e) {
                    is CancellationException -> LibraryScanUi(
                        outcome = LibraryScanOutcome.CANCELLED,
                        message = "Scan cancelled. Partial results may have been saved.",
                        esdeLinked = esdeLinked,
                    )
                    else -> LibraryScanUi(
                        outcome = LibraryScanOutcome.FAILED,
                        message = e.message ?: "Scan failed",
                        esdeLinked = esdeLinked,
                    )
                }
            }
        }
    }

    fun abort() {
        libraryRepository.requestCancelScan()
        scanJob?.cancel()
    }

    private fun formatSuccess(
        result: RomScanResult,
        summary: LibraryScanSummary,
        esdeLinked: Boolean,
    ): String =
        when {
            summary.gamesFound > 0 ->
                buildString {
                    append(
                        "Found ${summary.gamesFound} games across ${summary.systemsWithGames} systems",
                    )
                    append(
                        " (${summary.gamesWithMetadata} with metadata, " +
                            "${summary.gamesWithMedia} with media)",
                    )
                    when {
                        summary.mediaLinked > 0 ->
                            append(", linked ${summary.mediaLinked} media files")
                        esdeLinked -> append(
                            ". No media linked — check ES-DE data folder contains downloaded_media/",
                        )
                        else -> append(". No media linked — link ES-DE or scrape artwork")
                    }
                    if (esdeLinked) append(" ES-DE metadata and media were included in this scan.")
                    if (result.unknownFiles.isNotEmpty()) {
                        append(" ${result.unknownFiles.size} unrecognized files.")
                    }
                }
            result.systemsScanned == 0 ->
                "No system folders found under the selected directory. " +
                    "Pick the ROMs root that contains nes/, snes/, psx/, …"
            else ->
                "Scanned ${result.systemsScanned} systems but found 0 matching ROM files."
        }
}
