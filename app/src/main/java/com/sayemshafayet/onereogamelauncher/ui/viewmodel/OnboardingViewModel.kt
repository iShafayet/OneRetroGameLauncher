package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.MainActivity
import com.sayemshafayet.onereogamelauncher.data.homebrew.FreeHomebrewCatalog
import com.sayemshafayet.onereogamelauncher.data.homebrew.FreeHomebrewDownloadResult
import com.sayemshafayet.onereogamelauncher.data.homebrew.FreeHomebrewDownloader
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglExternalSync
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.BeginnerLibraryPreview
import com.sayemshafayet.onereogamelauncher.domain.DetectedEmulator
import com.sayemshafayet.onereogamelauncher.domain.LibraryScanSummary
import com.sayemshafayet.onereogamelauncher.domain.OnboardingStep
import com.sayemshafayet.onereogamelauncher.domain.OnboardingType
import com.sayemshafayet.onereogamelauncher.domain.RomsStructureCheck
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.launch.EmulatorLauncher
import com.sayemshafayet.onereogamelauncher.launch.RetroArchLauncher
import com.sayemshafayet.onereogamelauncher.legal.LegalDocuments
import com.sayemshafayet.onereogamelauncher.legal.isLegalAccepted
import com.sayemshafayet.onereogamelauncher.library.RomsRootStructureChecker
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
import com.sayemshafayet.onereogamelauncher.ui.util.OnboardingDirConflict
import com.sayemshafayet.onereogamelauncher.ui.util.SafFolderAccess
import com.sayemshafayet.onereogamelauncher.ui.util.SafPathResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface OnboardingRaPhase {
    data object Idle : OnboardingRaPhase
    data object Checking : OnboardingRaPhase
    data object Connected : OnboardingRaPhase
    data object Form : OnboardingRaPhase
}

/** UI destination inside the wizard (includes welcome variants not persisted as steps). */
enum class OnboardingUiPage {
    WELCOME,
    WELCOME_RESUME,
    TOS,
    FORK,
    PRO_ROMS,
    PRO_ORGL,
    PRO_RA,
    PRO_ESDE,
    PRO_SCAN,
    PRO_EMULATORS,
    PRO_DONE,
    BEGINNER_ORGL,
    BEGINNER_HAVE_ROMS,
    BEGINNER_ROMS_SETUP,
    BEGINNER_ROMS_SUMMARY,
    BEGINNER_NO_ROMS_HELP,
    BEGINNER_FREE_GAMES,
    BEGINNER_COMING_SOON,
}

data class OnboardingUiState(
    val page: OnboardingUiPage = OnboardingUiPage.WELCOME,
    val hydrated: Boolean = false,
    val romsUri: String? = null,
    val romsPath: String? = null,
    val romsAccessOk: Boolean = false,
    val romsPreselected: Boolean = false,
    val orglUri: String? = null,
    val orglPath: String? = null,
    val orglAccessOk: Boolean = false,
    val orglPreselected: Boolean = false,
    val orglReused: Boolean = false,
    val orglError: String? = null,
    val orglIncompatibleAlert: String? = null,
    val orglConflictError: String? = null,
    val esdeUri: String? = null,
    val esdePath: String? = null,
    val esdeAccessOk: Boolean = false,
    val esdePreselected: Boolean = false,
    val raUser: String = "",
    val raPassword: String = "",
    val raStoreOnDisk: Boolean = true,
    val raPhase: OnboardingRaPhase = OnboardingRaPhase.Idle,
    val raFoundOnDisk: Boolean = false,
    val raStatusMessage: String? = null,
    val raError: String? = null,
    val raSaving: Boolean = false,
    val scanning: Boolean = false,
    val scanError: String? = null,
    val scanDone: Boolean = false,
    val scanSummary: LibraryScanSummary? = null,
    val buildingSummary: Boolean = false,
    val detectedEmulators: List<DetectedEmulator> = emptyList(),
    val detectingEmulators: Boolean = false,
    // Beginner flow
    val beginnerStructureChecking: Boolean = false,
    val beginnerStructureCheck: RomsStructureCheck? = null,
    val beginnerStructureError: String? = null,
    val beginnerScanning: Boolean = false,
    val beginnerPreview: BeginnerLibraryPreview? = null,
    val beginnerScanError: String? = null,
    val freeGamesDownloading: Boolean = false,
    val freeGamesStatus: String? = null,
    val freeGamesError: String? = null,
    val freeGamesDownloaded: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val orglExternalSync: OrglExternalSync,
    private val raClient: RetroAchievementsClient,
    private val emulatorLauncher: EmulatorLauncher,
    private val retroArchLauncher: RetroArchLauncher,
    private val romsRootStructureChecker: RomsRootStructureChecker,
    private val freeHomebrewDownloader: FreeHomebrewDownloader,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    val scanProgress: StateFlow<ScanProgress?> = libraryRepository.scanProgress
    val freeHomebrewGames = FreeHomebrewCatalog.games
    val legalRomsGuides = FreeHomebrewCatalog.LEGAL_ROMS_GUIDES

    private var scanJob: Job? = null
    private var beginnerScanJob: Job? = null
    private var freeGamesJob: Job? = null

    init {
        viewModelScope.launch { hydrate() }
    }

    private suspend fun hydrate() {
        val settings = settingsRepository.settings.first()
        if (settings.onboardingDone) {
            _state.update { it.copy(hydrated = true) }
            return
        }

        val romsUri = settings.romsDirUri
        val orglUri = settings.orglDataDirUri
        val esdeUri = settings.esdeDataDirUri
        val romsOk = SafFolderAccess.isTreeAccessible(context, romsUri, requireWrite = false)
        val orglOk = SafFolderAccess.isTreeAccessible(context, orglUri, requireWrite = true)
        val esdeOk = SafFolderAccess.isTreeAccessible(context, esdeUri, requireWrite = false)

        val page = if (!settings.onboardingStarted) {
            OnboardingUiPage.WELCOME
        } else {
            OnboardingUiPage.WELCOME_RESUME
        }

        _state.update {
            it.copy(
                hydrated = true,
                page = page,
                romsUri = romsUri?.takeIf { romsOk },
                romsPath = settings.romsDirPath?.takeIf { romsOk },
                romsAccessOk = romsOk,
                romsPreselected = romsOk,
                orglUri = orglUri?.takeIf { orglOk },
                orglPath = settings.orglDataDirPath?.takeIf { orglOk },
                orglAccessOk = orglOk,
                orglPreselected = orglOk,
                esdeUri = esdeUri?.takeIf { esdeOk },
                esdePath = settings.esdeDataDirPath?.takeIf { esdeOk },
                esdeAccessOk = esdeOk,
                esdePreselected = esdeOk,
            )
        }
    }

    fun startJourney() {
        viewModelScope.launch {
            settingsRepository.setOnboardingStarted(true)
            val settings = settingsRepository.current()
            val next = firstStepAfterWelcome(
                settings.legalAcceptedVersion,
                settings.onboardingType,
                settings.onboardingStep,
            )
            goTo(next, persist = true)
        }
    }

    fun resumeJourney() {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val next = firstStepAfterWelcome(
                settings.legalAcceptedVersion,
                settings.onboardingType,
                settings.onboardingStep,
            )
            goTo(next, persist = true)
        }
    }

    fun restartJourney() {
        viewModelScope.launch {
            settingsRepository.resetOnboardingJourney()
            restartAppProcess()
        }
    }

    private fun restartAppProcess() {
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(launch)
        Runtime.getRuntime().exit(0)
    }

    private fun firstStepAfterWelcome(
        legalVersion: Int,
        type: OnboardingType,
        savedStep: OnboardingStep,
    ): OnboardingUiPage {
        if (!isLegalAccepted(legalVersion)) return OnboardingUiPage.TOS
        return when (type) {
            OnboardingType.NONE -> OnboardingUiPage.FORK
            OnboardingType.BEGINNER -> when (savedStep) {
                OnboardingStep.TOS,
                OnboardingStep.FORK,
                OnboardingStep.BEGINNER_STUB,
                -> OnboardingUiPage.BEGINNER_ORGL
                else -> savedStep.toUiPage()
            }
            OnboardingType.PRO -> when (savedStep) {
                OnboardingStep.TOS, OnboardingStep.FORK -> OnboardingUiPage.PRO_ROMS
                else -> savedStep.toUiPage()
            }
        }
    }

    fun acceptTos() {
        viewModelScope.launch {
            settingsRepository.setLegalAcceptedVersion(LegalDocuments.VERSION)
            val settings = settingsRepository.current()
            // Legal is now accepted; pick the next non-TOS destination.
            val next = when (settings.onboardingType) {
                OnboardingType.NONE -> OnboardingUiPage.FORK
                OnboardingType.BEGINNER -> when (settings.onboardingStep) {
                    OnboardingStep.TOS, OnboardingStep.FORK, OnboardingStep.BEGINNER_STUB ->
                        OnboardingUiPage.BEGINNER_ORGL
                    else -> settings.onboardingStep.toUiPage()
                }
                OnboardingType.PRO -> when (settings.onboardingStep) {
                    OnboardingStep.TOS, OnboardingStep.FORK -> OnboardingUiPage.PRO_ROMS
                    else -> settings.onboardingStep.toUiPage()
                }
            }
            goTo(next, persist = true)
        }
    }

    fun choosePro() {
        viewModelScope.launch {
            settingsRepository.setOnboardingType(OnboardingType.PRO)
            goTo(OnboardingUiPage.PRO_ROMS, persist = true)
        }
    }

    fun chooseBeginner() {
        viewModelScope.launch {
            settingsRepository.setOnboardingType(OnboardingType.BEGINNER)
            goTo(OnboardingUiPage.BEGINNER_ORGL, persist = true)
        }
    }

    fun backToFork() {
        viewModelScope.launch {
            settingsRepository.setOnboardingType(OnboardingType.NONE)
            goTo(OnboardingUiPage.FORK, persist = true)
        }
    }

    fun nextPage() {
        val current = _state.value.page
        when (current) {
            OnboardingUiPage.PRO_ROMS -> if (_state.value.romsUri == null) return
            OnboardingUiPage.PRO_ORGL -> if (!canAdvanceFromOrgl()) return
            OnboardingUiPage.PRO_SCAN -> if (!_state.value.scanDone) return
            OnboardingUiPage.BEGINNER_ORGL -> if (!canAdvanceFromOrgl()) return
            OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> {
                val preview = _state.value.beginnerPreview
                if (preview == null || preview.gamesFound <= 0) return
            }
            else -> Unit
        }
        val next = when (current) {
            OnboardingUiPage.PRO_ROMS -> OnboardingUiPage.PRO_ORGL
            OnboardingUiPage.PRO_ORGL -> OnboardingUiPage.PRO_RA
            OnboardingUiPage.PRO_RA -> OnboardingUiPage.PRO_ESDE
            OnboardingUiPage.PRO_ESDE -> OnboardingUiPage.PRO_SCAN
            OnboardingUiPage.PRO_SCAN -> OnboardingUiPage.PRO_EMULATORS
            OnboardingUiPage.PRO_EMULATORS -> OnboardingUiPage.PRO_DONE
            OnboardingUiPage.BEGINNER_ORGL -> OnboardingUiPage.BEGINNER_HAVE_ROMS
            OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> OnboardingUiPage.BEGINNER_COMING_SOON
            OnboardingUiPage.BEGINNER_FREE_GAMES -> {
                if (_state.value.freeGamesDownloaded) {
                    OnboardingUiPage.BEGINNER_ROMS_SUMMARY
                } else {
                    current
                }
            }
            else -> current
        }
        if (next == current) return
        goTo(next, persist = true)
    }

    fun prevPage() {
        val current = _state.value.page
        if (current == OnboardingUiPage.PRO_ROMS || current == OnboardingUiPage.BEGINNER_ORGL) {
            backToFork()
            return
        }
        val prev = when (current) {
            OnboardingUiPage.PRO_ORGL -> OnboardingUiPage.PRO_ROMS
            OnboardingUiPage.PRO_RA -> OnboardingUiPage.PRO_ORGL
            OnboardingUiPage.PRO_ESDE -> OnboardingUiPage.PRO_RA
            OnboardingUiPage.PRO_SCAN -> OnboardingUiPage.PRO_ESDE
            OnboardingUiPage.PRO_EMULATORS -> OnboardingUiPage.PRO_SCAN
            OnboardingUiPage.PRO_DONE -> OnboardingUiPage.PRO_EMULATORS
            OnboardingUiPage.BEGINNER_HAVE_ROMS -> OnboardingUiPage.BEGINNER_ORGL
            OnboardingUiPage.BEGINNER_ROMS_SETUP -> OnboardingUiPage.BEGINNER_HAVE_ROMS
            OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> OnboardingUiPage.BEGINNER_ROMS_SETUP
            OnboardingUiPage.BEGINNER_NO_ROMS_HELP -> OnboardingUiPage.BEGINNER_HAVE_ROMS
            OnboardingUiPage.BEGINNER_FREE_GAMES -> {
                // Prefer returning to whatever branch led here.
                if (_state.value.beginnerPreview != null) {
                    OnboardingUiPage.BEGINNER_ROMS_SUMMARY
                } else {
                    OnboardingUiPage.BEGINNER_NO_ROMS_HELP
                }
            }
            OnboardingUiPage.BEGINNER_COMING_SOON -> OnboardingUiPage.BEGINNER_ROMS_SUMMARY
            else -> return
        }
        goTo(prev, persist = true)
    }

    private fun canAdvanceFromOrgl(): Boolean {
        val s = _state.value
        if (s.orglUri == null) return false
        if (s.romsUri != null &&
            OnboardingDirConflict.conflicts(s.romsPath, s.orglPath, s.romsUri, s.orglUri)
        ) {
            _state.update {
                it.copy(
                    orglConflictError =
                        "ORGL data folder can't be the same as your ROMs folder, or inside/above it. " +
                            "Pick a separate folder (for example ORGL-Data next to your ROMs root).",
                )
            }
            return false
        }
        return true
    }

    private fun goTo(page: OnboardingUiPage, persist: Boolean) {
        _state.update { it.copy(page = page, orglConflictError = null) }
        if (persist) {
            page.toPersistedStep()?.let { step ->
                viewModelScope.launch { settingsRepository.setOnboardingStep(step) }
            }
        }
        when (page) {
            OnboardingUiPage.PRO_RA -> prepareRetroAchievements()
            OnboardingUiPage.PRO_SCAN -> ensureScanRunningOrSummary()
            OnboardingUiPage.PRO_EMULATORS -> detectEmulators()
            OnboardingUiPage.BEGINNER_ROMS_SETUP -> {
                if (_state.value.romsUri != null && _state.value.beginnerStructureCheck == null) {
                    runBeginnerStructureCheck(thenScanIfValid = false)
                }
            }
            OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> {
                val s = _state.value
                if (s.beginnerPreview == null &&
                    !s.beginnerScanning &&
                    s.romsUri != null &&
                    s.orglUri != null
                ) {
                    // Resume into summary: rescan current ROMs root (never reuse stale Room rows).
                    viewModelScope.launch {
                        performBeginnerLibraryScan(navigateToSummary = false)
                    }
                }
            }
            else -> Unit
        }
    }

    fun beginnerHaveRomsYes() {
        goTo(OnboardingUiPage.BEGINNER_ROMS_SETUP, persist = true)
    }

    fun beginnerHaveRomsNo() {
        goTo(OnboardingUiPage.BEGINNER_NO_ROMS_HELP, persist = true)
    }

    fun openBeginnerFreeGames() {
        goTo(OnboardingUiPage.BEGINNER_FREE_GAMES, persist = true)
    }

    fun onBeginnerRomsFolderPicked(uri: Uri) {
        val takeFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
        val pathHint = SafPathResolver.resolvePath(context, uri)
        val uriString = uri.toString()
        val current = _state.value
        if (OnboardingDirConflict.conflicts(pathHint, current.orglPath, uriString, current.orglUri)) {
            _state.update {
                it.copy(
                    beginnerStructureError =
                        "Your ROMs folder can't be the same as the ORGL data folder, or inside/above it. " +
                            "Pick a separate ROMs root.",
                    beginnerStructureCheck = null,
                )
            }
            return
        }
        _state.update {
            it.copy(
                romsUri = uriString,
                romsPath = pathHint,
                romsAccessOk = true,
                romsPreselected = false,
                beginnerStructureCheck = null,
                beginnerStructureError = null,
                beginnerPreview = null,
                beginnerScanError = null,
            )
        }
        viewModelScope.launch {
            settingsRepository.setRomsDir(uriString, pathHint)
            if (_state.value.page == OnboardingUiPage.BEGINNER_ROMS_SETUP) {
                val check = performBeginnerStructureCheck()
                if (check?.isValid == true) {
                    performBeginnerLibraryScan(navigateToSummary = true)
                }
            } else {
                // Free-games path: just remember the folder; user triggers download.
                _state.update {
                    it.copy(
                        beginnerStructureCheck = null,
                        beginnerStructureError = null,
                    )
                }
            }
        }
    }

    fun rescanBeginnerRomsStructure() {
        viewModelScope.launch {
            val check = performBeginnerStructureCheck()
            if (check?.isValid == true) {
                performBeginnerLibraryScan(navigateToSummary = true)
            }
        }
    }

    fun rescanBeginnerLibrary() {
        viewModelScope.launch {
            performBeginnerLibraryScan(navigateToSummary = true)
        }
    }

    private fun runBeginnerStructureCheck(thenScanIfValid: Boolean) {
        viewModelScope.launch {
            val check = performBeginnerStructureCheck()
            if (check?.isValid == true && thenScanIfValid) {
                performBeginnerLibraryScan(navigateToSummary = true)
            }
        }
    }

    private suspend fun performBeginnerStructureCheck(): RomsStructureCheck? {
        val s = _state.value
        if (s.romsUri == null) {
            _state.update {
                it.copy(beginnerStructureError = "Choose a ROMs folder first.")
            }
            return null
        }
        _state.update {
            it.copy(
                beginnerStructureChecking = true,
                beginnerStructureError = null,
            )
        }
        libraryRepository.ensureCatalogLoaded()
        val known = libraryRepository.systemFolderNames()
        val check = withContext(Dispatchers.IO) {
            romsRootStructureChecker.check(context, s.romsUri, s.romsPath, known)
        }
        _state.update {
            it.copy(
                beginnerStructureChecking = false,
                beginnerStructureCheck = check,
                beginnerStructureError = if (!check.isValid) {
                    "We didn’t find system folders like nes, gba, or snes in this directory."
                } else {
                    null
                },
            )
        }
        return check
    }

    private suspend fun performBeginnerLibraryScan(navigateToSummary: Boolean) {
        // Always run a fresh scan — never reuse an in-flight job's older result.
        beginnerScanJob?.cancel()
        beginnerScanJob?.join()
        val current = _state.value
        val romsUri = current.romsUri ?: return
        val orglUri = current.orglUri ?: return
        val job = viewModelScope.launch {
            settingsRepository.setRomsDir(romsUri, current.romsPath)
            settingsRepository.setOrglDataDir(orglUri, current.orglPath)
            _state.update {
                it.copy(
                    beginnerScanning = true,
                    beginnerScanError = null,
                    beginnerPreview = null,
                )
            }
            libraryRepository.ensureCatalogLoaded()
            runCatching { libraryRepository.scanLibrary() }
                .onSuccess {
                    runCatching { orglExternalSync.loadDuringSetup() }
                    val preview = libraryRepository.buildBeginnerLibraryPreview()
                    _state.update {
                        it.copy(
                            beginnerScanning = false,
                            beginnerPreview = preview,
                            beginnerScanError = null,
                        )
                    }
                    if (navigateToSummary) {
                        goTo(OnboardingUiPage.BEGINNER_ROMS_SUMMARY, persist = true)
                    }
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    runCatching { orglExternalSync.loadDuringSetup() }
                    _state.update {
                        it.copy(
                            beginnerScanning = false,
                            beginnerPreview = null,
                            beginnerScanError = e.message
                                ?: "Scan failed — check your ROMs folder and try again.",
                        )
                    }
                }
        }
        beginnerScanJob = job
        job.join()
    }

    fun downloadFreeHomebrewGames() {
        if (freeGamesJob?.isActive == true) return
        val romsUri = _state.value.romsUri
        if (romsUri == null) {
            _state.update {
                it.copy(freeGamesError = "Choose a ROMs folder first so we know where to save the games.")
            }
            return
        }
        freeGamesJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    freeGamesDownloading = true,
                    freeGamesError = null,
                    freeGamesStatus = "Downloading legal free starter games…",
                    freeGamesDownloaded = false,
                    // Drop any preview from an earlier ROMs pick in this session.
                    beginnerPreview = null,
                    beginnerScanError = null,
                )
            }
            var anyFailure: String? = null
            var successCount = 0
            for (game in FreeHomebrewCatalog.games) {
                _state.update {
                    it.copy(freeGamesStatus = "Downloading ${game.title}…")
                }
                when (
                    val result = freeHomebrewDownloader.downloadToRomsRoot(
                        context = context,
                        romsUri = romsUri,
                        romsPath = _state.value.romsPath,
                        game = game,
                    )
                ) {
                    is FreeHomebrewDownloadResult.Success -> successCount++
                    is FreeHomebrewDownloadResult.Failed -> {
                        anyFailure = result.message
                        break
                    }
                }
            }
            if (successCount == 0) {
                _state.update {
                    it.copy(
                        freeGamesDownloading = false,
                        freeGamesDownloaded = false,
                        freeGamesError = anyFailure ?: "Download failed.",
                        freeGamesStatus = null,
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    freeGamesStatus = "Download complete. Scanning your library…",
                    freeGamesError = anyFailure,
                    beginnerPreview = null,
                )
            }
            val check = performBeginnerStructureCheck()
            if (check?.isValid == true) {
                performBeginnerLibraryScan(navigateToSummary = true)
            } else {
                _state.update {
                    it.copy(
                        freeGamesError = it.beginnerStructureError
                            ?: "Downloaded games, but the ROMs folder layout still looks wrong.",
                    )
                }
            }
            _state.update {
                it.copy(
                    freeGamesDownloading = false,
                    freeGamesDownloaded = successCount > 0,
                    freeGamesStatus = if (successCount > 0 && it.beginnerPreview != null) {
                        "Starter games are ready."
                    } else if (successCount > 0) {
                        "Download finished."
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun dismissOrglIncompatibleAlert() {
        _state.update { it.copy(orglIncompatibleAlert = null) }
    }

    fun onRomsFolderPicked(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
        val pathHint = SafPathResolver.resolvePath(context, uri)
        val uriString = uri.toString()
        _state.update {
            it.copy(
                romsUri = uriString,
                romsPath = pathHint,
                romsAccessOk = true,
                romsPreselected = false,
                orglConflictError = null,
            )
        }
        viewModelScope.launch {
            settingsRepository.setRomsDir(uriString, pathHint)
        }
    }

    fun onOrglFolderPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update {
                it.copy(orglError = null, orglIncompatibleAlert = null, orglConflictError = null)
            }
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            val pathHint = SafPathResolver.resolvePath(context, uri)
            val uriString = uri.toString()
            val current = _state.value
            if (OnboardingDirConflict.conflicts(current.romsPath, pathHint, current.romsUri, uriString)) {
                _state.update {
                    it.copy(
                        orglUri = null,
                        orglPath = null,
                        orglAccessOk = false,
                        orglPreselected = false,
                        orglConflictError =
                            "ORGL data folder can't be the same as your ROMs folder, or inside/above it. " +
                                "Pick a separate folder (for example ORGL-Data next to your ROMs root).",
                    )
                }
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                OrglDataDirectory.prepare(context, uri)
            }
            when (result) {
                is OrglDataDirectory.PrepareResult.Ready -> {
                    settingsRepository.setOrglDataDir(uriString, pathHint)
                    _state.update {
                        it.copy(
                            orglUri = uriString,
                            orglPath = pathHint,
                            orglAccessOk = true,
                            orglPreselected = false,
                            orglReused = result.reusedExisting,
                            orglError = null,
                            orglIncompatibleAlert = null,
                            orglConflictError = null,
                            raPhase = OnboardingRaPhase.Idle,
                            raFoundOnDisk = false,
                            raStatusMessage = null,
                            raError = null,
                        )
                    }
                }
                is OrglDataDirectory.PrepareResult.Incompatible -> {
                    _state.update {
                        it.copy(
                            orglUri = null,
                            orglPath = null,
                            orglAccessOk = false,
                            orglPreselected = false,
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
                            orglAccessOk = false,
                            orglPreselected = false,
                            orglReused = false,
                            orglError = result.message,
                            orglIncompatibleAlert = null,
                        )
                    }
                }
            }
        }
    }

    fun onEsdeFolderPicked(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
        val pathHint = SafPathResolver.resolvePath(context, uri)
        val uriString = uri.toString()
        _state.update {
            it.copy(
                esdeUri = uriString,
                esdePath = pathHint,
                esdeAccessOk = true,
                esdePreselected = false,
            )
        }
        viewModelScope.launch {
            settingsRepository.setEsdeDataDir(uriString, pathHint)
        }
    }

    fun clearEsdeFolder() {
        _state.update {
            it.copy(
                esdeUri = null,
                esdePath = null,
                esdeAccessOk = false,
                esdePreselected = false,
            )
        }
        viewModelScope.launch {
            settingsRepository.clearEsdeDataDir()
        }
    }

    fun updateRaUser(value: String) {
        _state.update {
            it.copy(
                raUser = value,
                raError = null,
                raStatusMessage = if (it.raPhase == OnboardingRaPhase.Connected) null else it.raStatusMessage,
                raPhase = if (it.raPhase == OnboardingRaPhase.Connected) OnboardingRaPhase.Form else it.raPhase,
            )
        }
    }

    fun updateRaPassword(value: String) {
        _state.update {
            it.copy(
                raPassword = value,
                raError = null,
                raStatusMessage = if (it.raPhase == OnboardingRaPhase.Connected) null else it.raStatusMessage,
                raPhase = if (it.raPhase == OnboardingRaPhase.Connected) OnboardingRaPhase.Form else it.raPhase,
            )
        }
    }

    fun setRaStoreOnDisk(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRetroAchievementsStoreOnDisk(enabled)
            _state.update { it.copy(raStoreOnDisk = enabled) }
            if (!enabled) {
                runCatching { orglExternalSync.clearCredentialsOnDisk() }
            } else {
                val s = _state.value
                if (s.raPhase == OnboardingRaPhase.Connected &&
                    s.raUser.isNotBlank() &&
                    s.raPassword.isNotBlank()
                ) {
                    runCatching { orglExternalSync.saveCredentialsToDiskIfAllowed() }
                }
            }
        }
    }

    fun prepareRetroAchievements() {
        viewModelScope.launch {
            val orglUri = _state.value.orglUri ?: return@launch
            val tree = runCatching { Uri.parse(orglUri) }.getOrNull() ?: return@launch
            _state.update {
                it.copy(
                    raPhase = OnboardingRaPhase.Checking,
                    raError = null,
                    raStatusMessage = null,
                    raSaving = false,
                )
            }
            val creds = withContext(Dispatchers.IO) {
                orglExternalSync.peekCredentials(tree, _state.value.orglPath)
            }
            if (creds == null) {
                _state.update {
                    it.copy(
                        raPhase = OnboardingRaPhase.Form,
                        raFoundOnDisk = false,
                        raPassword = "",
                        raStoreOnDisk = true,
                        raStatusMessage = null,
                    )
                }
                settingsRepository.setRetroAchievementsStoreOnDisk(true)
                return@launch
            }

            _state.update {
                it.copy(
                    raFoundOnDisk = true,
                    raUser = creds.user,
                    raPassword = creds.password,
                    raStoreOnDisk = true,
                    raStatusMessage = "Found saved credentials for ${creds.user}. Signing in…",
                )
            }
            settingsRepository.setRetroAchievementsStoreOnDisk(true)
            raClient.verifyCredentials(creds.user, creds.password).fold(
                onSuccess = {
                    settingsRepository.setRetroAchievements(creds.user, creds.password)
                    if (creds.token.isNotBlank()) {
                        settingsRepository.setRetroAchievementsToken(creds.token)
                    }
                    runCatching { orglExternalSync.saveCredentialsToDiskIfAllowed() }
                    _state.update {
                        it.copy(
                            raPhase = OnboardingRaPhase.Connected,
                            raStatusMessage = "Found credentials for ${creds.user} and signed in.",
                            raError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            raPhase = OnboardingRaPhase.Form,
                            raUser = creds.user,
                            raPassword = "",
                            raStatusMessage = "Found saved credentials for ${creds.user}, but login failed. Enter your password to continue.",
                            raError = error.message,
                        )
                    }
                },
            )
        }
    }

    fun saveRetroAchievements() {
        viewModelScope.launch {
            val user = _state.value.raUser.trim()
            val password = _state.value.raPassword
            if (user.isBlank() || password.isBlank()) return@launch
            _state.update { it.copy(raSaving = true, raError = null) }
            raClient.verifyCredentials(user, password).fold(
                onSuccess = {
                    settingsRepository.setRetroAchievements(user, password.trim())
                    settingsRepository.setRetroAchievementsStoreOnDisk(_state.value.raStoreOnDisk)
                    if (_state.value.raStoreOnDisk) {
                        runCatching { orglExternalSync.saveCredentialsToDiskIfAllowed() }
                    } else {
                        runCatching { orglExternalSync.clearCredentialsOnDisk() }
                    }
                    _state.update {
                        it.copy(
                            raSaving = false,
                            raPhase = OnboardingRaPhase.Connected,
                            raPassword = password.trim(),
                            raStatusMessage = "Signed in as $user.",
                            raError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            raSaving = false,
                            raPhase = OnboardingRaPhase.Form,
                            raError = error.message ?: "Invalid username or password",
                        )
                    }
                },
            )
        }
    }

    private fun ensureScanRunningOrSummary() {
        if (_state.value.scanDone && _state.value.scanSummary != null) return
        if (scanJob?.isActive == true) return
        viewModelScope.launch {
            val existingGames = libraryRepository.countGames()
            if (existingGames > 0) {
                _state.update { it.copy(buildingSummary = true, scanError = null) }
                val summary = libraryRepository.buildLibraryScanSummary()
                _state.update {
                    it.copy(
                        buildingSummary = false,
                        scanning = false,
                        scanDone = true,
                        scanSummary = summary,
                    )
                }
                return@launch
            }
            startInitialScan()
        }
    }

    fun startInitialScan() {
        if (scanJob?.isActive == true) return
        val current = _state.value
        val romsUri = current.romsUri ?: return
        val orglUri = current.orglUri ?: return
        scanJob = viewModelScope.launch {
            settingsRepository.setRomsDir(romsUri, current.romsPath)
            settingsRepository.setOrglDataDir(orglUri, current.orglPath)
            val esdeUri = current.esdeUri
            if (esdeUri != null) {
                settingsRepository.setEsdeDataDir(esdeUri, current.esdePath)
            } else {
                settingsRepository.clearEsdeDataDir()
            }
            settingsRepository.setPreferredRetroArchPackage(RetroArchLauncher.DEFAULT_PACKAGE)
            _state.update {
                it.copy(
                    scanning = true,
                    scanError = null,
                    scanDone = false,
                    scanSummary = null,
                    buildingSummary = false,
                )
            }
            libraryRepository.ensureCatalogLoaded()
            runCatching { libraryRepository.scanLibrary() }
                .onSuccess { result ->
                    runCatching { orglExternalSync.loadDuringSetup() }
                    _state.update { it.copy(scanning = false, buildingSummary = true) }
                    val summary = libraryRepository.buildLibraryScanSummary(
                        unknownFiles = result.unknownFiles.size,
                    )
                    _state.update {
                        it.copy(
                            buildingSummary = false,
                            scanDone = true,
                            scanSummary = summary,
                            scanError = null,
                        )
                    }
                }
                .onFailure { e ->
                    runCatching { orglExternalSync.loadDuringSetup() }
                    _state.update {
                        it.copy(
                            scanning = false,
                            buildingSummary = false,
                            scanDone = false,
                            scanSummary = null,
                            scanError = e.message
                                ?: "Scan failed — check your ROMs folder and try again.",
                        )
                    }
                }
        }
    }

    fun detectEmulators() {
        viewModelScope.launch {
            _state.update { it.copy(detectingEmulators = true) }
            val found = withContext(Dispatchers.IO) {
                libraryRepository.ensureCatalogLoaded()
                libraryRepository.refreshEmulatorInstallState()
                val preferred = settingsRepository.current().preferredRetroArchPackage
                val standalone = EmulatorLauncher.SUPPORTED_PROFILES
                    .mapNotNull { profile ->
                        val resolved = emulatorLauncher.installedForKey(profile.key, preferred)
                            ?: return@mapNotNull null
                        DetectedEmulator(
                            key = profile.key,
                            label = profile.displayName,
                            packageName = resolved.packageName,
                        )
                    }
                    .distinctBy { it.packageName }
                val raPkgs = retroArchLauncher.installedPackages().map { pkg ->
                    DetectedEmulator(
                        key = "RETROARCH",
                        label = "RetroArch",
                        packageName = pkg,
                    )
                }
                (raPkgs + standalone)
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
            _state.update {
                it.copy(
                    detectingEmulators = false,
                    detectedEmulators = found,
                )
            }
        }
    }

    fun finalizeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingDone(true)
            settingsRepository.setPreferredRetroArchPackage(RetroArchLauncher.DEFAULT_PACKAGE)
            onComplete()
        }
    }

    private fun OnboardingStep.toUiPage(): OnboardingUiPage = when (this) {
        OnboardingStep.TOS -> OnboardingUiPage.TOS
        OnboardingStep.FORK -> OnboardingUiPage.FORK
        OnboardingStep.PRO_ROMS -> OnboardingUiPage.PRO_ROMS
        OnboardingStep.PRO_ORGL -> OnboardingUiPage.PRO_ORGL
        OnboardingStep.PRO_RA -> OnboardingUiPage.PRO_RA
        OnboardingStep.PRO_ESDE -> OnboardingUiPage.PRO_ESDE
        OnboardingStep.PRO_SCAN -> OnboardingUiPage.PRO_SCAN
        OnboardingStep.PRO_EMULATORS -> OnboardingUiPage.PRO_EMULATORS
        OnboardingStep.PRO_DONE -> OnboardingUiPage.PRO_DONE
        OnboardingStep.BEGINNER_ORGL, OnboardingStep.BEGINNER_STUB -> OnboardingUiPage.BEGINNER_ORGL
        OnboardingStep.BEGINNER_HAVE_ROMS -> OnboardingUiPage.BEGINNER_HAVE_ROMS
        OnboardingStep.BEGINNER_ROMS_SETUP -> OnboardingUiPage.BEGINNER_ROMS_SETUP
        OnboardingStep.BEGINNER_ROMS_SUMMARY -> OnboardingUiPage.BEGINNER_ROMS_SUMMARY
        OnboardingStep.BEGINNER_NO_ROMS_HELP -> OnboardingUiPage.BEGINNER_NO_ROMS_HELP
        OnboardingStep.BEGINNER_FREE_GAMES -> OnboardingUiPage.BEGINNER_FREE_GAMES
        OnboardingStep.BEGINNER_COMING_SOON -> OnboardingUiPage.BEGINNER_COMING_SOON
    }

    private fun OnboardingUiPage.toPersistedStep(): OnboardingStep? = when (this) {
        OnboardingUiPage.TOS -> OnboardingStep.TOS
        OnboardingUiPage.FORK -> OnboardingStep.FORK
        OnboardingUiPage.PRO_ROMS -> OnboardingStep.PRO_ROMS
        OnboardingUiPage.PRO_ORGL -> OnboardingStep.PRO_ORGL
        OnboardingUiPage.PRO_RA -> OnboardingStep.PRO_RA
        OnboardingUiPage.PRO_ESDE -> OnboardingStep.PRO_ESDE
        OnboardingUiPage.PRO_SCAN -> OnboardingStep.PRO_SCAN
        OnboardingUiPage.PRO_EMULATORS -> OnboardingStep.PRO_EMULATORS
        OnboardingUiPage.PRO_DONE -> OnboardingStep.PRO_DONE
        OnboardingUiPage.BEGINNER_ORGL -> OnboardingStep.BEGINNER_ORGL
        OnboardingUiPage.BEGINNER_HAVE_ROMS -> OnboardingStep.BEGINNER_HAVE_ROMS
        OnboardingUiPage.BEGINNER_ROMS_SETUP -> OnboardingStep.BEGINNER_ROMS_SETUP
        OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> OnboardingStep.BEGINNER_ROMS_SUMMARY
        OnboardingUiPage.BEGINNER_NO_ROMS_HELP -> OnboardingStep.BEGINNER_NO_ROMS_HELP
        OnboardingUiPage.BEGINNER_FREE_GAMES -> OnboardingStep.BEGINNER_FREE_GAMES
        OnboardingUiPage.BEGINNER_COMING_SOON -> OnboardingStep.BEGINNER_COMING_SOON
        OnboardingUiPage.WELCOME, OnboardingUiPage.WELCOME_RESUME -> null
    }
}
