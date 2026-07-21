package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameCompletedStatus
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadKeys
import com.sayemshafayet.onereogamelauncher.ui.input.OrglTabStrip
import com.sayemshafayet.onereogamelauncher.ui.input.cycleTabIndex
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.components.CoreDropdown
import com.sayemshafayet.onereogamelauncher.ui.components.EmulatorDropdown
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.GameVideoPlayer
import com.sayemshafayet.onereogamelauncher.ui.components.mediaTypeLabel
import com.sayemshafayet.onereogamelauncher.ui.components.RetroAchievementsButton
import com.sayemshafayet.onereogamelauncher.ui.components.pickBoxArt
import com.sayemshafayet.onereogamelauncher.ui.util.combinedLastPlayed
import com.sayemshafayet.onereogamelauncher.ui.util.combinedLaunchCount
import com.sayemshafayet.onereogamelauncher.ui.util.formatActivityLabel
import com.sayemshafayet.onereogamelauncher.ui.util.formatDate
import com.sayemshafayet.onereogamelauncher.ui.util.formatReleaseYear
import com.sayemshafayet.onereogamelauncher.ui.util.starsLabel
import com.sayemshafayet.onereogamelauncher.ui.util.totalOrglPlaytimeMs
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameDetailViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameLaunchConfigUi
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameRaUiState
import kotlinx.coroutines.launch

private enum class GameDetailTab { GAME, MEDIA, CONFIG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onOpenRetroAchievements: (Long) -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val game by viewModel.game.collectAsState()
    val media by viewModel.media.collectAsState()
    val launchConfig by viewModel.launchConfig.collectAsState()
    val activeCommitment by viewModel.activeCommitment.collectAsState()
    val commitmentPlaytimeMs by viewModel.commitmentPlaytimeMs.collectAsState()
    val raUi by viewModel.raUi.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onScreenResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by remember { mutableIntStateOf(GameDetailTab.GAME.ordinal) }
    var notes by remember(game?.notes) { mutableStateOf(game?.notes.orEmpty()) }
    var launchAllowed by remember { mutableStateOf(true) }
    var lockReason by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(game?.id, activeCommitment?.id) {
        launchAllowed = viewModel.launchAllowed()
        lockReason = if (!launchAllowed) {
            "Another game is committed in Play mode. Finish or drop it first."
        } else {
            null
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            when {
                GamepadKeys.isShoulderLeft(event) -> {
                    selectedTab = cycleTabIndex(selectedTab, -1, GameDetailTab.entries.size)
                    true
                }
                GamepadKeys.isShoulderRight(event) -> {
                    selectedTab = cycleTabIndex(selectedTab, 1, GameDetailTab.entries.size)
                    true
                }
                else -> false
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OrglTabStrip(
                labels = listOf("Game", "Media", "Config"),
                selectedIndex = selectedTab,
            )

            Box(Modifier.fillMaxSize()) {
                when (selectedTab) {
                    GameDetailTab.GAME.ordinal -> game?.let { g ->
                        GameTabContent(
                            game = g,
                            media = media,
                            commitmentPlaytimeMs = commitmentPlaytimeMs,
                            notes = notes,
                            onNotesChange = { notes = it },
                            launchAllowed = launchAllowed,
                            lockReason = lockReason,
                            raUi = raUi,
                            onOpenRetroAchievements = { onOpenRetroAchievements(g.id) },
                            onLaunch = {
                                scope.launch {
                                    val err = viewModel.launch()
                                    if (err != null) snackbar.showSnackbar(err)
                                    else snackbar.showSnackbar("Launched")
                                }
                            },
                            onSaveNotes = { viewModel.saveNotes(notes) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onToggleFinished = viewModel::toggleFinished,
                            onToggleDropped = viewModel::toggleDropped,
                        )
                    }
                    GameDetailTab.MEDIA.ordinal -> game?.let { g ->
                        MediaTabContent(game = g, media = media)
                    }
                    GameDetailTab.CONFIG.ordinal -> ConfigTabContent(
                        launchConfig = launchConfig,
                        onUseOverrideChange = viewModel::setUseOverride,
                        onEmulatorSelected = viewModel::setEmulatorKey,
                        onCoreSelected = viewModel::setCore,
                        onCoreTextChange = viewModel::setCore,
                        onCustomConfigChange = viewModel::setCustomConfigPath,
                        onSaveOverride = viewModel::saveLaunchConfig,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTabContent(
    game: GameEntity,
    media: List<MediaEntity>,
    commitmentPlaytimeMs: Long,
    notes: String,
    onNotesChange: (String) -> Unit,
    launchAllowed: Boolean,
    lockReason: String?,
    raUi: GameRaUiState,
    onOpenRetroAchievements: () -> Unit,
    onLaunch: () -> Unit,
    onSaveNotes: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFinished: () -> Unit,
    onToggleDropped: () -> Unit,
) {
    val launchFocus = rememberOrlgFocusRequester()
    val cover = pickBoxArt(media.associate { it.type to it.path })
    val activityLabel = formatActivityLabel(
        playtimeMs = game.totalOrglPlaytimeMs(commitmentPlaytimeMs),
        launchCount = game.combinedLaunchCount(),
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                MetadataLine("Genre", game.genre)
                MetadataLine("Developer", game.developer)
                MetadataLine("Publisher", game.publisher)
                formatReleaseYear(game.releaseDate)?.let { year ->
                    MetadataLine("Released", year)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onLaunch,
                    enabled = launchAllowed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(launchFocus),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Launch")
                }
                lockReason?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                RetroAchievementsButton(
                    state = raUi.button,
                    loading = raUi.loading,
                    onClick = onOpenRetroAchievements,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatBlock("Last played", formatDate(game.combinedLastPlayed()))
                    StatBlock("Activity", activityLabel)
                }

                StatusChips(
                    favorite = game.favorite,
                    completedStatus = game.completedStatus,
                    onToggleFavorite = onToggleFavorite,
                    onToggleFinished = onToggleFinished,
                    onToggleDropped = onToggleDropped,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Notes", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Personal notes for this game",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = { Text("Add your thoughts…") },
                )
                OutlinedButton(onClick = onSaveNotes) {
                    Text("Save notes")
                }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
    OrlgInitialFocus(launchFocus)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips(
    favorite: Boolean,
    completedStatus: GameCompletedStatus?,
    onToggleFavorite: () -> Unit,
    onToggleFinished: () -> Unit,
    onToggleDropped: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = favorite,
            onClick = onToggleFavorite,
            label = { Text("Favorite") },
            leadingIcon = {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp),
                )
            },
        )
        FilterChip(
            selected = completedStatus == GameCompletedStatus.FINISHED,
            onClick = onToggleFinished,
            label = { Text("Done") },
        )
        FilterChip(
            selected = completedStatus == GameCompletedStatus.DROPPED,
            onClick = onToggleDropped,
            label = { Text("Dropped") },
        )
    }
}

@Composable
private fun MetadataLine(label: String, value: String?) {
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
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MediaTabContent(
    game: GameEntity,
    media: List<MediaEntity>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (media.isEmpty()) {
            Text(
                "No media found for this game.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Artwork & video", style = MaterialTheme.typography.titleMedium)
            media.sortedBy { it.type.ordinal }.forEach { item ->
                MediaItemCard(item)
            }
        }

        MetadataSection(game)

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun MediaItemCard(item: MediaEntity) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            mediaTypeLabel(item.type),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (item.type) {
            MediaType.VIDEO -> {
                GameVideoPlayer(
                    path = item.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
            MediaType.FANART -> {
                GameCoverImage(
                    path = item.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
            MediaType.MARQUEE, MediaType.TITLE -> {
                GameCoverImage(
                    path = item.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
            }
            else -> {
                GameCoverImage(
                    path = item.path,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (item.type == MediaType.SCREENSHOT) 16f / 9f else 0.75f),
                )
            }
        }
    }
}

@Composable
private fun MetadataSection(game: GameEntity) {
    val hasMetadata = listOfNotNull(
        game.description,
        game.genre,
        game.developer,
        game.publisher,
        formatReleaseYear(game.releaseDate),
        game.players,
        game.rating?.let { starsLabel(it) },
    ).any { it.isNotBlank() }

    if (!hasMetadata) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Metadata", style = MaterialTheme.typography.titleMedium)
            game.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(desc, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
            MetadataRow("Genre", game.genre)
            MetadataRow("Developer", game.developer)
            MetadataRow("Publisher", game.publisher)
            MetadataRow("Release", formatReleaseYear(game.releaseDate))
            MetadataRow("Players", game.players)
            game.rating?.let { rating ->
                MetadataRow("Rating", starsLabel(rating))
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Composable
private fun ConfigTabContent(
    launchConfig: GameLaunchConfigUi,
    onUseOverrideChange: (Boolean) -> Unit,
    onEmulatorSelected: (String) -> Unit,
    onCoreSelected: (String) -> Unit,
    onCoreTextChange: (String) -> Unit,
    onCustomConfigChange: (String) -> Unit,
    onSaveOverride: () -> Unit,
) {
    val isRetroArch = launchConfig.emulatorKey.equals("RETROARCH", ignoreCase = true)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Launch configuration", style = MaterialTheme.typography.titleMedium)
        Text(
            buildString {
                append("System default: ${launchConfig.systemEmulatorLabel}")
                if (launchConfig.systemCoreLabel.isNotBlank() &&
                    launchConfig.systemCoreLabel != "Not set"
                ) {
                    append(" · ${launchConfig.systemCoreLabel}")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Per-game override")
                        Text(
                            "Use a different emulator or core for this game only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = launchConfig.useOverride,
                        onCheckedChange = onUseOverrideChange,
                    )
                }

                EmulatorDropdown(
                    choices = launchConfig.emulatorChoices,
                    selectedKey = launchConfig.emulatorKey,
                    onSelected = onEmulatorSelected,
                    enabled = launchConfig.useOverride,
                )

                if (isRetroArch) {
                    if (launchConfig.coreChoices.isNotEmpty()) {
                        CoreDropdown(
                            choices = launchConfig.coreChoices,
                            selectedCore = launchConfig.core,
                            onSelected = onCoreSelected,
                            enabled = launchConfig.useOverride,
                        )
                    }
                    OutlinedTextField(
                        value = launchConfig.core,
                        onValueChange = onCoreTextChange,
                        enabled = launchConfig.useOverride,
                        label = { Text("Core filename") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = launchConfig.customConfigPath,
                    onValueChange = onCustomConfigChange,
                    enabled = launchConfig.useOverride,
                    label = { Text("Custom config path (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (launchConfig.useOverride) {
                    OutlinedButton(onClick = onSaveOverride) {
                        Text("Save override")
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}
