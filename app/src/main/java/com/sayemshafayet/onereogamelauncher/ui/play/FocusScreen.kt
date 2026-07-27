package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.RaWithHltbRow
import com.sayemshafayet.onereogamelauncher.ui.components.StarRatingInput
import com.sayemshafayet.onereogamelauncher.ui.components.pickBoxArt
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintOverlay
import android.view.KeyEvent
import com.sayemshafayet.onereogamelauncher.ui.input.RegisterScreenButtonXHandler
import com.sayemshafayet.onereogamelauncher.ui.input.OrglKeyboardOptions
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgDpadFocusExit
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusChrome
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.input.rememberShowGamepadHints
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.util.combinedLastPlayed
import com.sayemshafayet.onereogamelauncher.ui.util.formatActivityLabel
import com.sayemshafayet.onereogamelauncher.ui.util.formatDate
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.FocusUiState
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.FocusViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameRaUiState

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
    val playInteraction = remember { MutableInteractionSource() }
    val showHints = rememberShowGamepadHints()

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
    val isWide = rememberIsWidePlayLayout()
    val playAction = remember(viewModel) { { viewModel.play {} } }

    RegisterScreenButtonXHandler { event ->
        if (event.action != KeyEvent.ACTION_UP || showReview || showMoreMenu) return@RegisterScreenButtonXHandler false
        when {
            game != null -> {
                playAction()
                true
            }
            else -> {
                onPickGame()
                true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PlayWideContainer {
            if (isWide && game != null) {
                FocusWideLayout(
                    game = game,
                    cover = cover,
                    state = state,
                    raUi = raUi,
                    activityLabel = activityLabel,
                    playFocus = playFocus,
                    playInteraction = playInteraction,
                    showHints = showHints,
                    showMoreMenu = showMoreMenu,
                    onShowMoreMenu = { showMoreMenu = it },
                    onPlay = playAction,
                    onOpenRetroAchievements = onOpenRetroAchievements,
                    onOpenJournal = onOpenJournal,
                    onReview = { status ->
                        reviewStatus = status
                        showReview = true
                    },
                )
            } else {
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
                            FocusableCover(
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
                        FocusActionsCard(
                            game = game,
                            state = state,
                            raUi = raUi,
                            activityLabel = activityLabel,
                            playFocus = playFocus,
                            playInteraction = playInteraction,
                            showHints = showHints,
                            showMoreMenu = showMoreMenu,
                            onShowMoreMenu = { showMoreMenu = it },
                            onPlay = playAction,
                            onOpenRetroAchievements = onOpenRetroAchievements,
                            onOpenJournal = onOpenJournal,
                            onReview = { status ->
                                reviewStatus = status
                                showReview = true
                            },
                        )
                    }

                    Spacer(Modifier.height(48.dp))
                }
            }
        }
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
                        keyboardOptions = OrglKeyboardOptions.Multiline,
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

/** Focusable cover so D-pad Up can reach the top of the scroll/page and bring it into view. */
@Composable
private fun FocusableCover(
    path: String?,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    GameCoverImage(
        path = path,
        modifier = modifier
            .focusable(interactionSource = interaction)
            .orlgFocusChrome(interaction, cornerRadius = 8.dp),
    )
}

@Composable
private fun FocusWideLayout(
    game: GameEntity,
    cover: String?,
    state: FocusUiState,
    raUi: GameRaUiState,
    activityLabel: String,
    playFocus: androidx.compose.ui.focus.FocusRequester,
    playInteraction: MutableInteractionSource,
    showHints: Boolean,
    showMoreMenu: Boolean,
    onShowMoreMenu: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onOpenRetroAchievements: (Long) -> Unit,
    onOpenJournal: () -> Unit,
    onReview: (CommitmentStatus) -> Unit,
) {
    val compact = rememberIsCompactWidePlayLayout()
    val padH = if (compact) 12.dp else 24.dp
    val padV = if (compact) 8.dp else 16.dp
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = padH, vertical = padV),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(if (compact) 0.34f else 0.4f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            FocusableCover(
                path = cover,
                modifier = Modifier
                    .fillMaxHeight(if (compact) 0.98f else 0.92f)
                    .aspectRatio(0.72f)
                    .widthIn(max = if (compact) 220.dp else 320.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(if (compact) 0.66f else 0.6f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            Text(
                "Now playing",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                game.title,
                style = if (compact) {
                    MaterialTheme.typography.titleLarge.copy(fontFamily = BrandFont)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont)
                },
                fontWeight = FontWeight.SemiBold,
                maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            state.system?.displayName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            FocusActionsCard(
                game = game,
                state = state,
                raUi = raUi,
                activityLabel = activityLabel,
                playFocus = playFocus,
                playInteraction = playInteraction,
                showHints = showHints,
                showMoreMenu = showMoreMenu,
                onShowMoreMenu = onShowMoreMenu,
                onPlay = onPlay,
                onOpenRetroAchievements = onOpenRetroAchievements,
                onOpenJournal = onOpenJournal,
                onReview = onReview,
                compact = compact,
            )
            if (!compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FocusMetadataLine("Genre", game.genre)
                    FocusMetadataLine("Developer", game.developer)
                }
            }
        }
    }
}

@Composable
private fun FocusActionsCard(
    game: GameEntity,
    state: FocusUiState,
    raUi: GameRaUiState,
    activityLabel: String,
    playFocus: androidx.compose.ui.focus.FocusRequester,
    playInteraction: MutableInteractionSource,
    showHints: Boolean,
    showMoreMenu: Boolean,
    onShowMoreMenu: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onOpenRetroAchievements: (Long) -> Unit,
    onOpenJournal: () -> Unit,
    onReview: (CommitmentStatus) -> Unit,
    compact: Boolean = false,
) {
    val moreInteraction = remember { MutableInteractionSource() }
    val actionHeight = if (compact) 48.dp else 52.dp

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onPlay,
                        interactionSource = playInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(actionHeight)
                            .focusRequester(playFocus),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Play", style = MaterialTheme.typography.titleMedium)
                    }
                    if (showHints) {
                        GamepadHintOverlay(
                            label = "X",
                            modifier = Modifier.align(Alignment.TopEnd),
                            offsetX = (-6).dp,
                            offsetY = (-6).dp,
                        )
                    }
                }
                Box {
                    FilledTonalIconButton(
                        onClick = { onShowMoreMenu(true) },
                        interactionSource = moreInteraction,
                        modifier = Modifier
                            .size(actionHeight)
                            .orlgFocusChrome(moreInteraction, cornerRadius = 50.dp),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { onShowMoreMenu(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as finished") },
                            onClick = {
                                onShowMoreMenu(false)
                                onReview(CommitmentStatus.FINISHED)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Give up") },
                            onClick = {
                                onShowMoreMenu(false)
                                onReview(CommitmentStatus.DROPPED)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = {
                                onShowMoreMenu(false)
                                onOpenJournal()
                            },
                        )
                    }
                }
            }

            state.launchError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            RaWithHltbRow(
                raState = raUi.button,
                raLoading = raUi.loading,
                onOpenRetroAchievements = { onOpenRetroAchievements(game.id) },
                hltbPhase = state.hltbPhase,
                hltbMainHours = state.hltb?.let {
                    it.mainHours ?: it.mainExtraHours ?: it.completionistHours
                },
                hltbVisible = state.hltbEnabled,
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp),
            ) {
                FocusStatBlock(
                    label = "Last played",
                    value = formatDate(game.combinedLastPlayed()),
                )
                FocusStatBlock(label = "This run", value = activityLabel)
            }
        }
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
