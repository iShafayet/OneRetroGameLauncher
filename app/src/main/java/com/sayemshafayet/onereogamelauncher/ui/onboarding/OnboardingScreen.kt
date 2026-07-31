package com.sayemshafayet.onereogamelauncher.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.domain.DetectedEmulator
import com.sayemshafayet.onereogamelauncher.domain.LibraryScanSummary
import com.sayemshafayet.onereogamelauncher.legal.LegalDocumentKind
import com.sayemshafayet.onereogamelauncher.ui.components.LibraryScanSummaryPanel
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.input.OrglKeyboardOptions
import com.sayemshafayet.onereogamelauncher.ui.input.orlgDpadFocusExit
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrglImeDismissActions
import com.sayemshafayet.onereogamelauncher.ui.legal.LegalAcceptanceScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerAdvancedFeaturesScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerDoneScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerEmulatorsScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerFreeGamesScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerHaveRomsScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerNoRomsHelpScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerOrglScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerRomsSetupScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerRomsSummaryScreen
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerSectionLabel
import com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner.BeginnerTryLaunchScreen
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.theme.InkDeep
import com.sayemshafayet.onereogamelauncher.ui.theme.InkLight
import com.sayemshafayet.onereogamelauncher.ui.theme.InkMid
import com.sayemshafayet.onereogamelauncher.ui.theme.Mist
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingRaPhase
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingUiPage
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingUiState
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenLegalDocument: (LegalDocumentKind) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    val context = LocalContext.current
    val romsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onRomsFolderPicked(it) }
    }
    val beginnerRomsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onBeginnerRomsFolderPicked(it) }
    }
    val orglPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onOrglFolderPicked(it) }
    }
    val esdePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onEsdeFolderPicked(it) }
    }

    state.orglIncompatibleAlert?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissOrglIncompatibleAlert,
            title = { Text("Incompatible ORGL folder") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissOrglIncompatibleAlert) {
                    Text("Choose another folder")
                }
            },
        )
    }

    if (!state.hydrated) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(InkDeep, InkMid, InkLight))),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AmberAccent)
        }
        return
    }

    if (state.page == OnboardingUiPage.TOS) {
        LegalAcceptanceScreen(
            onAccepted = viewModel::acceptTos,
            onOpenDocument = onOpenLegalDocument,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(InkDeep, InkMid, InkLight),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AnimatedContent(
            targetState = state.page,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "wizard",
        ) { page ->
            val scrollState = rememberScrollState()
            LaunchedEffect(page) {
                scrollState.scrollTo(0)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(scrollState)
                    .imePadding(),
            ) {
                Spacer(Modifier.height(16.dp))
                val beginnerPages = page == OnboardingUiPage.BEGINNER_ORGL ||
                    page == OnboardingUiPage.BEGINNER_HAVE_ROMS ||
                    page == OnboardingUiPage.BEGINNER_ROMS_SETUP ||
                    page == OnboardingUiPage.BEGINNER_ROMS_SUMMARY ||
                    page == OnboardingUiPage.BEGINNER_NO_ROMS_HELP ||
                    page == OnboardingUiPage.BEGINNER_FREE_GAMES ||
                    page == OnboardingUiPage.BEGINNER_EMULATORS ||
                    page == OnboardingUiPage.BEGINNER_TRY_LAUNCH ||
                    page == OnboardingUiPage.BEGINNER_ADVANCED ||
                    page == OnboardingUiPage.BEGINNER_DONE
                if (beginnerPages) {
                    BeginnerSectionLabel()
                } else {
                    Text(
                        "ORGL",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = BrandFont),
                        color = AmberAccent,
                    )
                }
                when (page) {
                    OnboardingUiPage.WELCOME -> WelcomeStep(resume = false)
                    OnboardingUiPage.WELCOME_RESUME -> WelcomeStep(resume = true)
                    OnboardingUiPage.FORK -> ForkStep(
                        onChooseBeginner = viewModel::chooseBeginner,
                        onChoosePro = viewModel::choosePro,
                    )
                    OnboardingUiPage.PRO_ROMS -> RomsFolderStep(
                        uri = state.romsUri,
                        pathHint = state.romsPath,
                        preselected = state.romsPreselected,
                        onPick = { romsPicker.launch(null) },
                    )
                    OnboardingUiPage.PRO_ORGL -> OrglFolderStep(
                        uri = state.orglUri,
                        pathHint = state.orglPath,
                        preselected = state.orglPreselected,
                        reused = state.orglReused,
                        error = state.orglError,
                        conflictError = state.orglConflictError,
                        onPick = { orglPicker.launch(null) },
                    )
                    OnboardingUiPage.PRO_RA -> RetroAchievementsStep(
                        state = state,
                        onUserChange = viewModel::updateRaUser,
                        onPasswordChange = viewModel::updateRaPassword,
                        onStoreOnDiskChange = viewModel::setRaStoreOnDisk,
                        onSave = viewModel::saveRetroAchievements,
                    )
                    OnboardingUiPage.PRO_ESDE -> EsdeFolderStep(
                        uri = state.esdeUri,
                        pathHint = state.esdePath,
                        preselected = state.esdePreselected,
                        onPick = { esdePicker.launch(null) },
                        onClear = viewModel::clearEsdeFolder,
                    )
                    OnboardingUiPage.PRO_SCAN -> ScanStep(
                        scanning = state.scanning,
                        buildingSummary = state.buildingSummary,
                        scanProgress = scanProgress,
                        scanError = state.scanError,
                        scanSummary = state.scanSummary,
                    )
                    OnboardingUiPage.PRO_EMULATORS -> EmulatorsStep(
                        detecting = state.detectingEmulators,
                        emulators = state.detectedEmulators,
                    )
                    OnboardingUiPage.PRO_DONE -> CongratsStep()
                    OnboardingUiPage.BEGINNER_ORGL -> BeginnerOrglScreen(
                        uri = state.orglUri,
                        pathHint = state.orglPath,
                        preselected = state.orglPreselected,
                        reused = state.orglReused,
                        error = state.orglError,
                        conflictError = state.orglConflictError,
                        onPick = { orglPicker.launch(null) },
                    )
                    OnboardingUiPage.BEGINNER_HAVE_ROMS -> BeginnerHaveRomsScreen(
                        onYes = viewModel::beginnerHaveRomsYes,
                        onNo = viewModel::beginnerHaveRomsNo,
                    )
                    OnboardingUiPage.BEGINNER_ROMS_SETUP -> BeginnerRomsSetupScreen(
                        uri = state.romsUri,
                        pathHint = state.romsPath,
                        preselected = state.romsPreselected,
                        structureChecking = state.beginnerStructureChecking,
                        structureCheck = state.beginnerStructureCheck,
                        structureError = state.beginnerStructureError,
                        scanning = state.beginnerScanning,
                        scanError = state.beginnerScanError,
                        onPick = { beginnerRomsPicker.launch(null) },
                    )
                    OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> BeginnerRomsSummaryScreen(
                        scanning = state.beginnerScanning,
                        preview = state.beginnerPreview,
                        scanError = state.beginnerScanError,
                        onGetFreeGames = viewModel::openBeginnerFreeGames,
                    )
                    OnboardingUiPage.BEGINNER_NO_ROMS_HELP -> BeginnerNoRomsHelpScreen(
                        guides = viewModel.legalRomsGuides,
                        onOpenGuide = { url ->
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                )
                            }
                        },
                        onGetFreeGames = viewModel::openBeginnerFreeGames,
                    )
                    OnboardingUiPage.BEGINNER_FREE_GAMES -> BeginnerFreeGamesScreen(
                        romsUri = state.romsUri,
                        romsPath = state.romsPath,
                        games = viewModel.freeHomebrewGames,
                        downloading = state.freeGamesDownloading || state.beginnerScanning,
                        status = state.freeGamesStatus,
                        error = state.freeGamesError ?: state.beginnerScanError,
                        downloaded = state.freeGamesDownloaded,
                        onPickRoms = { beginnerRomsPicker.launch(null) },
                        onDownload = viewModel::downloadFreeHomebrewGames,
                    )
                    OnboardingUiPage.BEGINNER_EMULATORS -> BeginnerEmulatorsScreen(
                        detecting = state.detectingEmulators,
                        emulators = state.detectedEmulators,
                        librarySystems = state.beginnerPreview?.systems
                            ?.map { "${it.displayName} (${it.folderName})" }
                            .orEmpty(),
                        systemCoreNeeds = state.beginnerSystemCoreNeeds,
                        onOpenUrl = { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                    )
                    OnboardingUiPage.BEGINNER_TRY_LAUNCH -> BeginnerTryLaunchScreen(
                        offer = state.beginnerTryLaunchOffer,
                        busy = state.tryLaunchBusy,
                        error = state.tryLaunchError,
                        launched = state.tryLaunchStarted,
                        onLaunch = viewModel::launchTryGame,
                        onOpenRetroArch = viewModel::openRetroArchForCoreDownload,
                    )
                    OnboardingUiPage.BEGINNER_ADVANCED -> BeginnerAdvancedFeaturesScreen()
                    OnboardingUiPage.BEGINNER_DONE -> BeginnerDoneScreen()
                    OnboardingUiPage.TOS -> Unit
                }

                Spacer(Modifier.height(24.dp))

                WizardFooter(
                    page = page,
                    state = state,
                    scanProgress = scanProgress,
                    onStart = viewModel::startJourney,
                    onResume = viewModel::resumeJourney,
                    onRestart = viewModel::restartJourney,
                    onBack = viewModel::prevPage,
                    onBackToFork = viewModel::backToFork,
                    onNext = viewModel::nextPage,
                    onRetryScan = viewModel::startInitialScan,
                    onRescanEmulators = viewModel::detectEmulators,
                    onRescanBeginnerStructure = viewModel::rescanBeginnerRomsStructure,
                    onRescanBeginnerLibrary = viewModel::rescanBeginnerLibrary,
                    onDownloadFreeGames = viewModel::downloadFreeHomebrewGames,
                    onFinish = { viewModel.finalizeOnboarding(onFinished) },
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WizardFooter(
    page: OnboardingUiPage,
    state: OnboardingUiState,
    scanProgress: com.sayemshafayet.onereogamelauncher.domain.ScanProgress?,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onBackToFork: () -> Unit,
    onNext: () -> Unit,
    onRetryScan: () -> Unit,
    onRescanEmulators: () -> Unit,
    onRescanBeginnerStructure: () -> Unit,
    onRescanBeginnerLibrary: () -> Unit,
    onDownloadFreeGames: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (page) {
            OnboardingUiPage.WELCOME -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Let's finish some games")
                }
            }

            OnboardingUiPage.WELCOME_RESUME -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Resume onboarding")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Restart my journey", color = Mist)
                }
            }

            OnboardingUiPage.PRO_SCAN -> {
                val pageBusy = state.scanning || state.buildingSummary
                if (pageBusy) {
                    CircularProgressIndicator(color = AmberAccent)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            state.buildingSummary -> "Building library summary…"
                            else -> scanProgress?.let {
                                "Scanning ${it.systemName}… ${it.gamesTotal} games"
                            } ?: "Preparing library…"
                        },
                        color = Mist.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (!pageBusy && (state.scanDone || state.scanError != null)) {
                    Button(
                        onClick = {
                            when {
                                state.scanDone -> onNext()
                                else -> onRetryScan()
                            }
                        },
                        enabled = when {
                            state.scanDone && state.scanSummary != null -> true
                            state.scanError != null ->
                                state.romsUri != null && state.orglUri != null
                            else -> false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAccent,
                            contentColor = InkDeep,
                        ),
                    ) {
                        Text(if (state.scanDone) "Continue" else "Retry scan")
                    }
                    if (state.scanDone) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRetryScan,
                            enabled = state.romsUri != null && state.orglUri != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Rescan", color = Mist)
                        }
                    }
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.PRO_DONE -> {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Enter ORGL")
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.PRO_ROMS,
            OnboardingUiPage.PRO_ORGL,
            OnboardingUiPage.PRO_RA,
            OnboardingUiPage.PRO_ESDE,
            -> {
                val ctaEnabled = when (page) {
                    OnboardingUiPage.PRO_ROMS -> state.romsUri != null
                    OnboardingUiPage.PRO_ORGL -> state.orglUri != null
                    OnboardingUiPage.PRO_RA ->
                        state.raPhase != OnboardingRaPhase.Checking && !state.raSaving
                    else -> true
                }
                Button(
                    onClick = onNext,
                    enabled = ctaEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text(
                        when (page) {
                            OnboardingUiPage.PRO_RA -> when (state.raPhase) {
                                OnboardingRaPhase.Connected -> "Continue"
                                OnboardingRaPhase.Checking -> "Checking…"
                                else -> "Skip for now"
                            }
                            OnboardingUiPage.PRO_ESDE ->
                                if (state.esdeUri != null) "Continue" else "Skip for now"
                            else -> "Continue"
                        },
                    )
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.PRO_EMULATORS -> {
                Button(
                    onClick = onNext,
                    enabled = !state.detectingEmulators,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Continue")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRescanEmulators,
                    enabled = !state.detectingEmulators,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Rescan", color = Mist)
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_ORGL -> {
                Button(
                    onClick = onNext,
                    enabled = state.orglUri != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Continue")
                }
                TextButton(onClick = onBackToFork) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_HAVE_ROMS,
            OnboardingUiPage.BEGINNER_NO_ROMS_HELP,
            OnboardingUiPage.FORK,
            -> {
                if (page != OnboardingUiPage.FORK) {
                    TextButton(onClick = onBack) {
                        Text("Back", color = Mist.copy(alpha = 0.7f))
                    }
                }
            }

            OnboardingUiPage.BEGINNER_ROMS_SETUP -> {
                val busy = state.beginnerStructureChecking || state.beginnerScanning
                if (busy) {
                    CircularProgressIndicator(color = AmberAccent)
                    Spacer(Modifier.height(12.dp))
                }
                val showRescan = state.romsUri != null &&
                    !busy &&
                    (state.beginnerStructureCheck?.isValid == false || state.beginnerScanError != null)
                if (showRescan) {
                    OutlinedButton(
                        onClick = onRescanBeginnerStructure,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Rescan", color = Mist)
                    }
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_ROMS_SUMMARY -> {
                val gamesOk = (state.beginnerPreview?.gamesFound ?: 0) > 0
                if (state.beginnerScanning) {
                    CircularProgressIndicator(color = AmberAccent)
                    Spacer(Modifier.height(12.dp))
                }
                if (!state.beginnerScanning && gamesOk) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAccent,
                            contentColor = InkDeep,
                        ),
                    ) {
                        Text("Continue")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (!state.beginnerScanning) {
                    OutlinedButton(
                        onClick = onRescanBeginnerLibrary,
                        enabled = state.romsUri != null && state.orglUri != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Rescan", color = Mist)
                    }
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_FREE_GAMES -> {
                val busy = state.freeGamesDownloading || state.beginnerScanning
                if (busy) {
                    CircularProgressIndicator(color = AmberAccent)
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = onDownloadFreeGames,
                    enabled = !busy && state.romsUri != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text(
                        if (state.freeGamesDownloaded) {
                            "Download again"
                        } else {
                            "Download legal free games"
                        },
                    )
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_EMULATORS -> {
                if (state.detectingEmulators) {
                    CircularProgressIndicator(color = AmberAccent)
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = onNext,
                    enabled = !state.detectingEmulators,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Continue")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRescanEmulators,
                    enabled = !state.detectingEmulators,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Rescan", color = Mist)
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_TRY_LAUNCH -> {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Continue")
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_ADVANCED -> {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Continue")
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            OnboardingUiPage.BEGINNER_DONE -> {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = InkDeep,
                    ),
                ) {
                    Text("Enter ORGL")
                }
                TextButton(onClick = onBack) {
                    Text("Back", color = Mist.copy(alpha = 0.7f))
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun WelcomeStep(resume: Boolean) {
    Spacer(Modifier.height(24.dp))
    Text(
        "One Retro\nGame Launcher",
        style = MaterialTheme.typography.displayMedium.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        if (resume) "Welcome back" else "Stop scrolling. Start finishing.",
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
        color = AmberAccent,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        if (resume) {
            "You left setup unfinished. Resume where you left off, or restart from the beginning."
        } else {
            "One game at a time — the only launcher built around commitment, not infinite browsing."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    if (!resume) {
        Spacer(Modifier.height(12.dp))
        Text(
            "ES-DE compatible. Your ROMs, your media, your rules. We never touch your files.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mist.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun ForkStep(
    onChooseBeginner: () -> Unit,
    onChoosePro: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "How should we set you up?",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Pick the path that matches where you are today. You can restart later if you change your mind.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(28.dp))
    PathChoice(
        title = "I need help getting started",
        body = "I don’t have an emulator set up on this device and I need help setting things up.",
        onClick = onChooseBeginner,
    )
    Spacer(Modifier.height(16.dp))
    PathChoice(
        title = "I already have everything",
        body = "I already have emulators and ROMs set up on this device.",
        onClick = onChoosePro,
    )
}

@Composable
private fun PathChoice(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Mist.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = BrandFont),
            color = AmberAccent,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = Mist.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun RomsFolderStep(
    uri: String?,
    pathHint: String?,
    preselected: Boolean,
    onPick: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Point us at your ROMs",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Pick the ROMs root — one subfolder per system (nes, snes, …). Read-only. ORGL never writes here.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    if (preselected) {
        Spacer(Modifier.height(12.dp))
        Text(
            "We still have access to a folder you linked before — confirm it or pick another.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(if (uri == null) "Choose ROMs folder" else "Change folder", color = Mist)
    }
    FolderStatus(pathHint = pathHint, uri = uri, accessOk = uri != null)
}

@Composable
private fun OrglFolderStep(
    uri: String?,
    pathHint: String?,
    preselected: Boolean,
    reused: Boolean,
    error: String?,
    conflictError: String?,
    onPick: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "ORGL data folder",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "This is not your ROMs folder. Pick a separate place for ORGL-owned files — scraped artwork, " +
            "credentials you choose to store, and other app data (downloaded_media/). " +
            "Keep it next to your ROMs root if you like, but never inside it.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "You can reuse a folder from a previous install — we’ll keep what’s already there. " +
            "ORGL writes a small ${OrglDataDirectory.META_FILE_NAME} marker (spec version " +
            "${OrglDataDirectory.SPEC_VERSION}) so future installs stay compatible.",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.7f),
    )
    if (preselected) {
        Spacer(Modifier.height(12.dp))
        Text(
            "We still have access to a folder you linked before — confirm it or pick another.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (uri == null) "Choose ORGL data folder" else "Change folder",
            color = Mist,
        )
    }
    FolderStatus(pathHint = pathHint, uri = uri, accessOk = uri != null)
    if (uri != null && reused) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Existing ORGL data detected — reusing scraped media and files from this folder.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    conflictError?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
    error?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EsdeFolderStep(
    uri: String?,
    pathHint: String?,
    preselected: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "ES-DE data folder",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Optional. Link ES-DE’s application data folder to reuse its downloaded_media/ artwork " +
            "and gamelists/ metadata. Read-only — ORGL never writes to ES-DE.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "You can skip this and link it later in Settings.",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.7f),
    )
    if (preselected) {
        Spacer(Modifier.height(12.dp))
        Text(
            "We still have access to a folder you linked before — confirm it or pick another.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (uri == null) "Choose ES-DE data folder" else "Change folder",
            color = Mist,
        )
    }
    if (uri != null) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClear) {
            Text("Clear selection", color = Mist.copy(alpha = 0.75f))
        }
    }
    FolderStatus(pathHint = pathHint, uri = uri, accessOk = uri != null, safHint = "Folder linked via SAF (read-only).")
}

@Composable
private fun FolderStatus(
    pathHint: String?,
    uri: String?,
    accessOk: Boolean,
    safHint: String = "Folder linked. Absolute path could not be resolved — scanning will use SAF access.",
) {
    if (pathHint != null) {
        Spacer(Modifier.height(12.dp))
        Text("Resolved path", color = AmberAccent, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(pathHint, style = MaterialTheme.typography.bodyMedium, color = Mist.copy(alpha = 0.85f))
        if (accessOk) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Access looks good.",
                style = MaterialTheme.typography.bodySmall,
                color = AmberAccent,
            )
        }
    } else if (uri != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            safHint,
            style = MaterialTheme.typography.bodySmall,
            color = Mist.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun RetroAchievementsStep(
    state: OnboardingUiState,
    onUserChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onStoreOnDiskChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Mist,
        unfocusedTextColor = Mist,
        disabledTextColor = Mist.copy(alpha = 0.7f),
        focusedBorderColor = AmberAccent,
        unfocusedBorderColor = Mist.copy(alpha = 0.35f),
        disabledBorderColor = Mist.copy(alpha = 0.2f),
        focusedLabelColor = AmberAccent,
        unfocusedLabelColor = Mist.copy(alpha = 0.7f),
        cursorColor = AmberAccent,
    )
    val busy = state.raPhase == OnboardingRaPhase.Checking || state.raSaving

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "RetroAchievements",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        "Optional. Use the same username and password as RetroArch. ORGL only reads progress — unlocks still happen in RetroArch.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )

    when (state.raPhase) {
        OnboardingRaPhase.Checking, OnboardingRaPhase.Idle -> {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = AmberAccent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                state.raStatusMessage ?: "Checking the ORGL data folder for saved credentials…",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
            )
        }
        OnboardingRaPhase.Connected -> {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                state.raStatusMessage ?: "Signed in.",
                style = MaterialTheme.typography.bodyLarge,
                color = AmberAccent,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Store on disk", color = Mist)
                    Text(
                        "Keep encrypted credentials in the ORGL data folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist.copy(alpha = 0.7f),
                    )
                }
                Switch(
                    checked = state.raStoreOnDisk,
                    onCheckedChange = onStoreOnDiskChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkDeep,
                        checkedTrackColor = AmberAccent,
                    ),
                )
            }
        }
        OnboardingRaPhase.Form -> {
            state.raStatusMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberAccent,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.raUser,
                onValueChange = onUserChange,
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .orlgDpadFocusExit(),
                singleLine = true,
                keyboardOptions = OrglKeyboardOptions.SingleLine,
                keyboardActions = rememberOrglImeDismissActions(),
                enabled = !busy,
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.raPassword,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = OrglKeyboardOptions.Password,
                keyboardActions = rememberOrglImeDismissActions(),
                modifier = Modifier
                    .fillMaxWidth()
                    .orlgDpadFocusExit(),
                singleLine = true,
                enabled = !busy,
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Store on disk", color = Mist)
                    Text(
                        "Save encrypted credentials into the ORGL data folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist.copy(alpha = 0.7f),
                    )
                }
                Switch(
                    checked = state.raStoreOnDisk,
                    onCheckedChange = onStoreOnDiskChange,
                    enabled = !busy,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkDeep,
                        checkedTrackColor = AmberAccent,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSave,
                enabled = !busy && state.raUser.isNotBlank() && state.raPassword.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.raSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = AmberAccent,
                    )
                } else {
                    Text("Save & verify", color = Mist)
                }
            }
            state.raError?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ScanStep(
    scanning: Boolean,
    buildingSummary: Boolean,
    scanProgress: com.sayemshafayet.onereogamelauncher.domain.ScanProgress?,
    scanError: String?,
    scanSummary: LibraryScanSummary?,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        when {
            scanSummary != null -> "Library ready"
            scanning || buildingSummary -> "Scanning library"
            scanError != null -> "Scan needed"
            else -> "You're set"
        },
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        when {
            scanSummary != null ->
                "Review what we found, then continue to check which emulators are installed."
            scanning ->
                "Scanning your ROMs folder. This finishes before you can continue."
            buildingSummary ->
                "Scan finished. Building the per-system breakdown…"
            scanError != null ->
                "Fix the issue below, then retry the scan."
            else ->
                "We'll scan your library next and show a full breakdown."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    scanError?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
    if (scanning && scanProgress != null) {
        Spacer(Modifier.height(16.dp))
        Text(
            "System ${scanProgress.systemsDone + 1} of ${scanProgress.systemsTotal}" +
                if (scanProgress.systemName.isNotBlank()) ": ${scanProgress.systemName}" else "",
            color = Mist.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "${scanProgress.gamesTotal} games · ${scanProgress.mediaTotal} media",
            color = Mist.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    scanSummary?.let { summary ->
        Spacer(Modifier.height(20.dp))
        LibraryScanSummaryPanel(
            summary = summary,
            labelColor = Mist.copy(alpha = 0.65f),
            valueColor = Mist,
            titleColor = Mist,
        )
    }
}

@Composable
private fun EmulatorsStep(
    detecting: Boolean,
    emulators: List<DetectedEmulator>,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Installed emulators",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Here’s what ORGL recognized on this device. You can refine per-system launchers later in Setup.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(20.dp))
    when {
        detecting -> {
            CircularProgressIndicator(color = AmberAccent)
            Spacer(Modifier.height(12.dp))
            Text(
                "Looking for installed emulators…",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
            )
        }
        emulators.isEmpty() -> {
            Text(
                "No recognized emulators found yet. You can still continue — install or link them from Setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
            )
        }
        else -> {
            emulators.forEach { emu ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        emu.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = Mist,
                    )
                    Text(
                        emu.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist.copy(alpha = 0.65f),
                    )
                    if (emu.key == "RETROARCH") {
                        when {
                            emu.coreQuerySupported == true && emu.installedCores != null -> {
                                val cores = emu.installedCores
                                Text(
                                    if (cores.isEmpty()) {
                                        "No cores installed yet"
                                    } else {
                                        "${cores.size} cores installed"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberAccent,
                                )
                            }
                            emu.coreQuerySupported == false -> {
                                Text(
                                    "Couldn’t list installed cores automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Mist.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CongratsStep() {
    Spacer(Modifier.height(24.dp))
    Text(
        "You're ready",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "Setup is complete. Commit to a game and start finishing.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
}
