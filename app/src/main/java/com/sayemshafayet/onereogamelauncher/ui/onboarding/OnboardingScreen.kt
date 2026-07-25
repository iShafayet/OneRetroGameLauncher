package com.sayemshafayet.onereogamelauncher.ui.onboarding

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.domain.LibraryScanSummary
import com.sayemshafayet.onereogamelauncher.ui.components.LibraryScanSummaryPanel
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.input.orlgDpadFocusExit
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.theme.InkDeep
import com.sayemshafayet.onereogamelauncher.ui.theme.InkLight
import com.sayemshafayet.onereogamelauncher.ui.theme.InkMid
import com.sayemshafayet.onereogamelauncher.ui.theme.Mist
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingRaPhase
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingUiState
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    val romsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onRomsFolderPicked(it) }
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
                Text(
                    "ORGL",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = BrandFont),
                    color = AmberAccent,
                )
                when (page) {
                    0 -> WelcomeStep()
                    1 -> RomsFolderStep(
                        uri = state.romsUri,
                        pathHint = state.romsPath,
                        onPick = { romsPicker.launch(null) },
                    )
                    2 -> OrglFolderStep(
                        uri = state.orglUri,
                        pathHint = state.orglPath,
                        reused = state.orglReused,
                        error = state.orglError,
                        onPick = { orglPicker.launch(null) },
                    )
                    3 -> RetroAchievementsStep(
                        state = state,
                        onUserChange = viewModel::updateRaUser,
                        onPasswordChange = viewModel::updateRaPassword,
                        onStoreOnDiskChange = viewModel::setRaStoreOnDisk,
                        onSave = viewModel::saveRetroAchievements,
                    )
                    4 -> EsdeFolderStep(
                        uri = state.esdeUri,
                        pathHint = state.esdePath,
                        onPick = { esdePicker.launch(null) },
                        onClear = viewModel::clearEsdeFolder,
                    )
                    5 -> DoneStep(
                        scanning = state.scanning,
                        buildingSummary = state.buildingSummary,
                        scanProgress = scanProgress,
                        scanError = state.scanError,
                        scanSummary = state.scanSummary,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (page == 5 && (state.scanning || state.buildingSummary)) {
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

                    val pageBusy = state.scanning || state.buildingSummary
                    val ctaEnabled = when (page) {
                        1 -> state.romsUri != null
                        2 -> state.orglUri != null
                        3 -> state.raPhase != OnboardingRaPhase.Checking && !state.raSaving
                        5 -> when {
                            pageBusy -> false
                            state.scanDone && state.scanSummary != null -> true
                            state.scanError != null ->
                                state.romsUri != null && state.orglUri != null
                            else -> false
                        }
                        else -> true
                    }

                    // Finalize only after summary; otherwise retry/start scan. Auto-scan starts on page entry.
                    if (page != 5 || state.scanDone || state.scanError != null) {
                        Button(
                            onClick = {
                                when (page) {
                                    5 -> when {
                                        state.scanDone -> viewModel.finalizeOnboarding(onFinished)
                                        else -> viewModel.startInitialScan()
                                    }
                                    else -> viewModel.nextPage()
                                }
                            },
                            enabled = ctaEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(PulseModifier(page == 0)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberAccent,
                                contentColor = InkDeep,
                            ),
                        ) {
                            Text(
                                when (page) {
                                    0 -> "Let's finish some games"
                                    1, 2 -> "Continue"
                                    3 -> when (state.raPhase) {
                                        OnboardingRaPhase.Connected -> "Continue"
                                        OnboardingRaPhase.Checking -> "Checking…"
                                        else -> "Skip for now"
                                    }
                                    4 -> if (state.esdeUri != null) "Continue" else "Skip for now"
                                    else -> when {
                                        state.scanDone -> "Enter ORGL"
                                        state.scanError != null -> "Retry scan"
                                        else -> "Scan library"
                                    }
                                },
                            )
                        }
                    }

                    if (page in 1..4) {
                        TextButton(onClick = { viewModel.prevPage() }) {
                            Text("Back", color = Mist.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(24.dp))
    Text(
        "One Retro\nGame Launcher",
        style = MaterialTheme.typography.displayMedium.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "Stop scrolling. Start finishing.",
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
        color = AmberAccent,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "One game at a time — the only launcher built around commitment, not infinite browsing.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "ES-DE compatible. Your ROMs, your media, your rules. We never touch your files.",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.72f),
    )
}

@Composable
private fun RomsFolderStep(
    uri: String?,
    pathHint: String?,
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
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(if (uri == null) "Choose ROMs folder" else "Change folder", color = Mist)
    }
    if (pathHint != null) {
        Spacer(Modifier.height(12.dp))
        Text("Resolved path", color = AmberAccent, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(pathHint, style = MaterialTheme.typography.bodyMedium, color = Mist.copy(alpha = 0.85f))
    } else if (uri != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Folder linked. Absolute path could not be resolved — scanning will use SAF access.",
            style = MaterialTheme.typography.bodySmall,
            color = Mist.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun OrglFolderStep(
    uri: String?,
    pathHint: String?,
    reused: Boolean,
    error: String?,
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
        "Required. Scraped artwork and ORGL-owned files live here (downloaded_media/). " +
            "You can reuse a folder from a previous install — we’ll keep what’s already there.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "ORGL writes a small ${OrglDataDirectory.META_FILE_NAME} marker (spec version " +
            "${OrglDataDirectory.SPEC_VERSION}) so future installs stay compatible.",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.7f),
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (uri == null) "Choose ORGL data folder" else "Change folder",
            color = Mist,
        )
    }
    if (pathHint != null) {
        Spacer(Modifier.height(12.dp))
        Text("Resolved path", color = AmberAccent, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(pathHint, style = MaterialTheme.typography.bodyMedium, color = Mist.copy(alpha = 0.85f))
    } else if (uri != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Folder linked via SAF.",
            style = MaterialTheme.typography.bodySmall,
            color = Mist.copy(alpha = 0.7f),
        )
    }
    if (uri != null && reused) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Existing ORGL data detected — reusing scraped media and files from this folder.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
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
    if (pathHint != null) {
        Spacer(Modifier.height(12.dp))
        Text("Resolved path", color = AmberAccent, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(pathHint, style = MaterialTheme.typography.bodyMedium, color = Mist.copy(alpha = 0.85f))
    } else if (uri != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Folder linked via SAF (read-only).",
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
                enabled = !busy,
                colors = fieldColors,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.raPassword,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
private fun DoneStep(
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
                "Review what we found. Enter ORGL when you're ready — your library is already loaded."
            scanning ->
                "Scanning your ROMs folder. This finishes before you can enter the app."
            buildingSummary ->
                "Scan finished. Building the per-system breakdown…"
            scanError != null ->
                "Fix the issue below, then retry the scan."
            else ->
                "We'll scan your library next and show a full breakdown before you enter."
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
