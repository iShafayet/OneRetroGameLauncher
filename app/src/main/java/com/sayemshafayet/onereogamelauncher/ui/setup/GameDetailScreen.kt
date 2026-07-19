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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.MediaType
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
    val config by viewModel.config.collectAsState()
    val emulators by viewModel.emulators.collectAsState()
    val activeCommitment by viewModel.activeCommitment.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var notes by remember(game?.description) { mutableStateOf(game?.description.orEmpty()) }
    var emulatorKey by remember(config?.emulatorKey) { mutableStateOf(config?.emulatorKey.orEmpty()) }
    var core by remember(config?.coreOverride) { mutableStateOf(config?.coreOverride.orEmpty()) }
    var configPath by remember(config?.customConfigPath) { mutableStateOf(config?.customConfigPath.orEmpty()) }
    var launchAllowed by remember { mutableStateOf(true) }
    var lockReason by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(game?.id, activeCommitment?.id) {
        launchAllowed = viewModel.launchAllowed()
        if (!launchAllowed) {
            lockReason = "Another game is committed in Play mode. Finish or drop it first."
        }
    }

    val hero = media.firstOrNull { it.type == MediaType.FANART }?.path
        ?: media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path

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
                OutlinedButton(onClick = { viewModel.scrapeGame() }) {
                    Text("Scrape artwork")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Emulator override", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = emulatorKey,
                onValueChange = { emulatorKey = it },
                label = { Text("Emulator key") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = core,
                onValueChange = { core = it },
                label = { Text("Core (RetroArch)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = configPath,
                onValueChange = { configPath = it },
                label = { Text("Custom config path") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    viewModel.saveConfig(
                        emulatorKey.ifBlank { null },
                        core.ifBlank { null },
                        configPath.ifBlank { null },
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Save config")
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
