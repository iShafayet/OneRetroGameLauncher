package com.sayemshafayet.onereogamelauncher.ui.play

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.components.RetroAchievementsButton
import com.sayemshafayet.onereogamelauncher.ui.components.StarRatingInput
import com.sayemshafayet.onereogamelauncher.ui.components.pickBoxArt
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgDpadFocusExit
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.util.combinedLastPlayed
import com.sayemshafayet.onereogamelauncher.ui.util.formatActivityLabel
import com.sayemshafayet.onereogamelauncher.ui.util.formatDate
import com.sayemshafayet.onereogamelauncher.ui.util.formatHours
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.FocusViewModel

@Composable
fun FocusScreen(
    onRunCompleted: () -> Unit,
    onPickGame: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenRetroAchievements: (Long) -> Unit,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val raUi by viewModel.raUi.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showReview by remember { mutableStateOf(false) }
    var reviewStatus by remember { mutableStateOf(CommitmentStatus.FINISHED) }
    var stars by remember { mutableFloatStateOf(4f) }
    var reviewText by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    val playFocus = rememberOrlgFocusRequester()

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

    val game = state.game
    val cover = pickBoxArt(state.media.associate { it.type to it.path })
    val activityLabel = formatActivityLabel(state.playtimeMs, state.sessionCount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            if (game != null) "Now playing" else "Slot ${state.slotIndex + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )

        if (game == null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No game in this slot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Pick a game to commit to this slot. Other slots are unaffected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Button(
                        onClick = onPickGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(playFocus),
                    ) {
                        Text("Pick a game")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GameCoverImage(
                    path = cover,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(0.75f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        game.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = BrandFont),
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.system?.displayName?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    FocusMetadataLine("Genre", game.genre)
                    FocusMetadataLine("Developer", game.developer)
                }
            }
        }

        if (game != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { viewModel.play {} },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusRequester(playFocus)
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Play", style = MaterialTheme.typography.titleMedium)
                }

                state.launchError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                game?.let { g ->
                    RetroAchievementsButton(
                        state = raUi.button,
                        loading = raUi.loading,
                        onClick = { onOpenRetroAchievements(g.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    FocusStatBlock(
                        label = "Last played",
                        value = formatDate(game?.combinedLastPlayed()),
                    )
                    FocusStatBlock(label = "This run", value = activityLabel)
                }

                state.hltb?.mainHours?.let { h ->
                    Text(
                        "HLTB main: ${formatHours(h)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .orlgFocusable(onClick = { showMoreMenu = true }),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("More options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as finished") },
                            onClick = {
                                showMoreMenu = false
                                reviewStatus = CommitmentStatus.FINISHED
                                showReview = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Give up") },
                            onClick = {
                                showMoreMenu = false
                                reviewStatus = CommitmentStatus.DROPPED
                                showReview = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = {
                                showMoreMenu = false
                                onOpenJournal()
                            },
                        )
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(48.dp))
    }

    OrlgInitialFocus(
        playFocus,
        enabled = game != null || state.commitment == null,
        resetKey = state.slotIndex to (game?.id ?: 0L),
    )

    if (showReview) {
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = {
                Text(if (reviewStatus == CommitmentStatus.FINISHED) "Finished!" else "Give up?")
            },
            text = {
                Column {
                    Text(
                        if (reviewStatus == CommitmentStatus.FINISHED) {
                            "Nice run. Release the lock and pick your next game when you're ready."
                        } else {
                            "That's okay — drop this one and you can pick something else."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    StarRatingInput(stars = stars, onStarsChange = { stars = it })
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("Review (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .orlgDpadFocusExit(),
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReview = false
                        if (reviewStatus == CommitmentStatus.FINISHED) {
                            viewModel.finish(stars, reviewText.ifBlank { null }, onRunCompleted)
                        } else {
                            viewModel.drop(stars, reviewText.ifBlank { null }, onRunCompleted)
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

@Composable
private fun FocusMetadataLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FocusStatBlock(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
