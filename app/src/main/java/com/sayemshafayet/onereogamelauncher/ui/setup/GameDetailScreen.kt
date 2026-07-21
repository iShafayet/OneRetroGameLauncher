package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.CoreDropdown
import com.sayemshafayet.onereogamelauncher.ui.components.EmulatorDropdown
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val game by viewModel.game.collectAsState()
    val media by viewModel.media.collectAsState()
    val launchConfig by viewModel.launchConfig.collectAsState()
    val activeCommitment by viewModel.activeCommitment.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var notes by remember(game?.description) { mutableStateOf(game?.description.orEmpty()) }
    var launchAllowed by remember { mutableStateOf(true) }
    var lockReason by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(game?.id, activeCommitment?.id) {
        launchAllowed = viewModel.launchAllowed()
        if (!launchAllowed) {
            lockReason = "Another game is committed in Play mode. Finish or drop it first."
        } else {
            lockReason = null
        }
    }

    val hero = media.firstOrNull { it.type == MediaType.FANART }?.path
        ?: media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
    val isRetroArch = launchConfig.emulatorKey.equals("RETROARCH", ignoreCase = true)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (game?.favorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            GameCoverImage(
                path = hero,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Spacer(Modifier.height(12.dp))
            game?.let { g ->
                Text(g.title, style = MaterialTheme.typography.headlineMedium)
                g.genre?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                g.description?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val onShelf = game?.onShelf == true
                OutlinedButton(onClick = { viewModel.toggleShelf(!onShelf) }) {
                    Icon(Icons.Default.StarOutline, contentDescription = null)
                    Text(if (onShelf) "On shelf" else "Add to shelf")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Launch", style = MaterialTheme.typography.titleMedium)
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Per-game override")
                    Text(
                        "Use a different emulator/core for this game only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = launchConfig.useOverride,
                    onCheckedChange = viewModel::setUseOverride,
                )
            }

            EmulatorDropdown(
                choices = launchConfig.emulatorChoices,
                selectedKey = launchConfig.emulatorKey,
                onSelected = viewModel::setEmulatorKey,
                enabled = launchConfig.useOverride,
            )
            if (isRetroArch) {
                if (launchConfig.coreChoices.isNotEmpty()) {
                    CoreDropdown(
                        choices = launchConfig.coreChoices,
                        selectedCore = launchConfig.core,
                        onSelected = viewModel::setCore,
                        enabled = launchConfig.useOverride,
                    )
                }
                OutlinedTextField(
                    value = launchConfig.core,
                    onValueChange = viewModel::setCore,
                    enabled = launchConfig.useOverride,
                    label = { Text("Core filename") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            OutlinedTextField(
                value = launchConfig.customConfigPath,
                onValueChange = viewModel::setCustomConfigPath,
                enabled = launchConfig.useOverride,
                label = { Text("Custom config path (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            if (launchConfig.useOverride) {
                OutlinedButton(
                    onClick = viewModel::saveLaunchConfig,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Save override")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Notes", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            OutlinedButton(onClick = { viewModel.saveNotes(notes) }) {
                Text("Save notes")
            }
            if (media.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Media", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(media, key = { it.id }) { m ->
                        GameCoverImage(
                            path = m.path,
                            modifier = Modifier
                                .height(100.dp)
                                .aspectRatio(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        val err = viewModel.launch()
                        if (err != null) snackbar.showSnackbar(err) else snackbar.showSnackbar("Launched")
                    }
                },
                enabled = launchAllowed && game != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Play")
            }
            lockReason?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
