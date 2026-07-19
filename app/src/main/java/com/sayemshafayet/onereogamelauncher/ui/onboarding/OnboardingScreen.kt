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

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { picked ->
            viewModel.onFolderPicked(picked)
        }
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
                        1 -> FolderStep(
                            uri = state.romsUri,
                            pathHint = state.romsPath,
                            onPick = { folderPicker.launch(null) },
                        )
                        2 -> CredentialsTeaseStep()
                        3 -> DoneStep(
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
                    if (page == 3 && state.scanning) {
                        CircularProgressIndicator(color = AmberAccent)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            scanProgress?.let {
                                "Scanning ${it.systemName}… ${it.gamesFound} games"
                            } ?: "Preparing library…",
                            color = Mist.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    val ctaEnabled = when (page) {
                        1 -> state.romsUri != null
                        3 -> !state.scanning
                        else -> true
                    }

                    Button(
                        onClick = {
                            when (page) {
                                3 -> viewModel.finishOnboarding(onFinished)
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
                                1 -> "Continue"
                                2 -> "Skip for now"
                                else -> if (state.scanning) "Scanning…" else "Enter ORGL"
                            },
                        )
                    }

                    if (page in 1..2) {
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
private fun FolderStep(
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
        "Pick the ROMs root — one subfolder per system (nes, snes, …). Read-only. Set ORGL’s data folder (and optional ES-DE data) later in Settings.",
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
