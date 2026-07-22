package com.sayemshafayet.onereogamelauncher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglExternalSync
import com.sayemshafayet.onereogamelauncher.data.prefs.SettingsRepository
import com.sayemshafayet.onereogamelauncher.data.repository.LibraryRepository
import com.sayemshafayet.onereogamelauncher.domain.ScanProgress
import com.sayemshafayet.onereogamelauncher.ra.RetroAchievementsClient
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

sealed interface OnboardingRaPhase {
    data object Idle : OnboardingRaPhase
    data object Checking : OnboardingRaPhase
    data object Connected : OnboardingRaPhase
    data object Form : OnboardingRaPhase
}

data class OnboardingUiState(
    val page: Int = 0,
    val romsUri: String? = null,
    val romsPath: String? = null,
    val orglUri: String? = null,
    val orglPath: String? = null,
    val orglReused: Boolean = false,
    val orglError: String? = null,
    val orglIncompatibleAlert: String? = null,
    val esdeUri: String? = null,
    val esdePath: String? = null,
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
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val orglExternalSync: OrglExternalSync,
    private val raClient: RetroAchievementsClient,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    val scanProgress: StateFlow<ScanProgress?> = libraryRepository.scanProgress

    fun nextPage() {
        val current = _state.value.page
        val next = (current + 1).coerceAtMost(LAST_PAGE)
        _state.update { it.copy(page = next) }
        if (next == PAGE_RA) {
            prepareRetroAchievements()
        }
    }

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
                    // Persist early so RA disk load/save works during the wizard.
                    settingsRepository.setOrglDataDir(uri.toString(), pathHint)
                    _state.update {
                        it.copy(
                            orglUri = uri.toString(),
                            orglPath = pathHint,
                            orglReused = result.reusedExisting,
                            orglError = null,
                            orglIncompatibleAlert = null,
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

    fun onEsdeFolderPicked(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }
        val pathHint = SafPathResolver.resolvePath(context, uri)
        _state.update {
            it.copy(
                esdeUri = uri.toString(),
                esdePath = pathHint,
            )
        }
    }

    fun clearEsdeFolder() {
        _state.update { it.copy(esdeUri = null, esdePath = null) }
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
                        raUser = it.raUser,
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
            val verified = raClient.verifyCredentials(creds.user, creds.password)
            verified.fold(
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

    fun finishOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value
            val romsUri = current.romsUri ?: return@launch
            val orglUri = current.orglUri ?: return@launch
            settingsRepository.setRomsDir(romsUri, current.romsPath)
            settingsRepository.setOrglDataDir(orglUri, current.orglPath)
            val esdeUri = current.esdeUri
            if (esdeUri != null) {
                settingsRepository.setEsdeDataDir(esdeUri, current.esdePath)
            } else {
                settingsRepository.clearEsdeDataDir()
            }
            settingsRepository.setOnboardingDone(true)
            _state.update { it.copy(scanning = true, scanError = null) }
            libraryRepository.ensureCatalogLoaded()
            runCatching { libraryRepository.scanLibrary() }
                .onSuccess {
                    runCatching { orglExternalSync.loadDuringSetup() }
                    _state.update { it.copy(scanning = false, scanDone = true) }
                    onComplete()
                }
                .onFailure { e ->
                    runCatching { orglExternalSync.loadDuringSetup() }
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

    companion object {
        const val LAST_PAGE = 5
        const val PAGE_RA = 3
        const val PAGE_ESDE = 4
        const val PAGE_DONE = 5
    }
}
