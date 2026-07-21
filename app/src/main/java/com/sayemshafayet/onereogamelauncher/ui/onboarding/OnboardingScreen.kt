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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.theme.InkDeep
import com.sayemshafayet.onereogamelauncher.ui.theme.InkLight
import com.sayemshafayet.onereogamelauncher.ui.theme.InkMid
import com.sayemshafayet.onereogamelauncher.ui.theme.Mist
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Spacer(Modifier.height(32.dp))
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
                        3 -> CredentialsTeaseStep()
                        4 -> DoneStep(
                            scanning = state.scanning,
                            scanProgress = scanProgress,
                            scanError = state.scanError,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (page == 4 && state.scanning) {
                        CircularProgressIndicator(color = AmberAccent)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            scanProgress?.let {
                                "Scanning ${it.systemName}… ${it.gamesTotal} games"
                            } ?: "Preparing library…",
                            color = Mist.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    val ctaEnabled = when (page) {
                        1 -> state.romsUri != null
                        2 -> state.orglUri != null
                        4 -> !state.scanning && state.romsUri != null && state.orglUri != null
                        else -> true
                    }

                    Button(
                        onClick = {
                            when (page) {
                                4 -> viewModel.finishOnboarding(onFinished)
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
                                3 -> "Skip for now"
                                else -> if (state.scanning) "Scanning…" else "Enter ORGL"
                            },
                        )
                    }

                    if (page in 1..3) {
                        TextButton(onClick = { viewModel.prevPage() }) {
                            Text("Back", color = Mist.copy(alpha = 0.7f))
                        }
                    }
                }
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
private fun CredentialsTeaseStep() {
    Spacer(Modifier.height(24.dp))
    Text(
        "Power-ups, optional",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "ScreenScraper for artwork, RetroAchievements for progress, HowLongToBeat for time estimates — all configurable later in Settings.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "No account required to start playing. Add credentials when you're ready.",
        style = MaterialTheme.typography.bodyMedium,
        color = AmberAccent,
    )
}

@Composable
private fun DoneStep(
    scanning: Boolean,
    scanProgress: com.sayemshafayet.onereogamelauncher.domain.ScanProgress?,
    scanError: String?,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "You're set",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        if (scanning) {
            "First scan running in the background…"
        } else {
            "Library ready. Pick Setup to browse and scrape, or Play to commit to your first game."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.85f),
    )
    scanError?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
}
