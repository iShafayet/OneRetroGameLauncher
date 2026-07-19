package com.sayemshafayet.onereogamelauncher.ui.play

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.components.StarRatingInput
import com.sayemshafayet.onereogamelauncher.ui.components.pickHeroMedia
import com.sayemshafayet.onereogamelauncher.ui.util.formatDurationMs
import com.sayemshafayet.onereogamelauncher.ui.util.formatHours
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.FocusViewModel
import java.io.File

@Composable
fun FocusScreen(
    onReleased: () -> Unit,
    onOpenJournal: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var showReview by remember { mutableStateOf(false) }
    var reviewStatus by remember { mutableStateOf(CommitmentStatus.FINISHED) }
    var stars by remember { mutableFloatStateOf(4f) }
    var reviewText by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onReturnFromEmulator()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.game?.id) {
        if (state.game != null && !state.extrasLoaded) viewModel.loadExtras()
    }

    val mediaMap = state.media.associate { it.type to it.path }
    val hero = pickHeroMedia(mediaMap)

    Box(Modifier.fillMaxSize()) {
        GameCoverImage(
            path = hero,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(
                    state.game?.title ?: "",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                state.system?.displayName?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${formatDurationMs(state.playtimeMs)} · ${state.sessionCount} sessions",
                    style = MaterialTheme.typography.bodyLarge,
                )
                state.hltb?.mainHours?.let { h ->
                    Text(
                        "HLTB main: ${formatHours(h)} · you've played ${formatHours(state.playtimeMs / 3_600_000.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.ra?.let { ra ->
                    if (ra.total > 0) {
                        Text(
                            "RetroAchievements ${ra.earned}/${ra.total}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                state.launchError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.play {} },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(32.dp))
                    Text("Play", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            reviewStatus = CommitmentStatus.FINISHED
                            showReview = true
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text("Finish")
                    }
                    OutlinedButton(
                        onClick = {
                            reviewStatus = CommitmentStatus.DROPPED
                            showReview = true
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Text("Drop")
                    }
                }
                TextButton(onClick = onOpenJournal) {
                    Text("Journal")
                }
            }
        }
    }

    if (showReview) {
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = {
                Text(if (reviewStatus == CommitmentStatus.FINISHED) "Finished!" else "Dropped")
            },
            text = {
                Column {
                    StarRatingInput(stars = stars, onStarsChange = { stars = it })
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("Review (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReview = false
                        val share: (String?) -> Unit = { path ->
                            path?.let { p ->
                                val file = File(p)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file,
                                    )
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            "Share your run",
                                        ),
                                    )
                                }
                            }
                            onReleased()
                        }
                        if (reviewStatus == CommitmentStatus.FINISHED) {
                            viewModel.finish(stars, reviewText.ifBlank { null }, share)
                        } else {
                            viewModel.drop(stars, reviewText.ifBlank { null }, share)
                        }
                    },
                ) {
                    Text("Release lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReview = false }) { Text("Cancel") }
            },
        )
    }
}
