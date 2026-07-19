package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.SearchField
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameListFilter
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SystemGamesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemGamesScreen(
    onGameClick: (Long) -> Unit,
    onEmulatorSettings: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SystemGamesViewModel = hiltViewModel(),
) {
    val system by viewModel.system.collectAsState()
    val games by viewModel.games.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.gameFilter.collectAsState()
    val layout by viewModel.layout.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(system?.displayName ?: "Games") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEmulatorSettings(viewModel.systemId) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Emulator settings")
                    }
                    IconButton(onClick = viewModel::toggleLayout) {
                        if (layout == GameListLayout.GRID) {
                            Icon(Icons.Default.ViewList, contentDescription = "List view")
                        } else {
                            Icon(Icons.Default.GridView, contentDescription = "Grid view")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = "Search games",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GameListFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { viewModel.setFilter(f) },
                        label = { Text(f.label) },
                    )
                }
            }
            when (layout) {
                GameListLayout.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(games, key = { it.id }) { game ->
                            GameGridTile(
                                game = game,
                                observeMedia = { viewModel.observeMedia(game.id) },
                                onClick = { onGameClick(game.id) },
                            )
                        }
                    }
                }
                GameListLayout.LIST -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(games, key = { it.id }) { game ->
                            GameListRow(
                                game = game,
                                observeMedia = { viewModel.observeMedia(game.id) },
                                onClick = { onGameClick(game.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameGridTile(
    game: GameEntity,
    observeMedia: () -> kotlinx.coroutines.flow.Flow<List<com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity>>,
    onClick: () -> Unit,
) {
    val media by observeMedia().collectAsState(initial = emptyList())
    val cover = coverPath(media)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        GameCoverImage(
            path = cover,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f),
        )
        Text(
            game.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun GameListRow(
    game: GameEntity,
    observeMedia: () -> kotlinx.coroutines.flow.Flow<List<com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity>>,
    onClick: () -> Unit,
) {
    val media by observeMedia().collectAsState(initial = emptyList())
    val cover = coverPath(media)

    ListItem(
        headlineContent = {
            Text(game.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(game.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            GameCoverImage(
                path = cover,
                modifier = Modifier
                    .width(48.dp)
                    .height(64.dp),
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
    HorizontalDivider()
}

private fun coverPath(
    media: List<com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity>,
): String? =
    media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
        ?: media.firstOrNull { it.type == MediaType.SCREENSHOT }?.path
        ?: media.firstOrNull()?.path

private val GameListFilter.label: String
    get() = when (this) {
        GameListFilter.ALL -> "All"
        GameListFilter.FAVORITE -> "★"
        GameListFilter.FINISHED -> "Done"
        GameListFilter.DROPPED -> "Dropped"
    }
