package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayPickerViewModel

@Composable
fun PlayPickerScreen(
    onGameSelected: (Long) -> Unit,
    viewModel: PlayPickerViewModel = hiltViewModel(),
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val shelf by viewModel.shelf.collectAsState()
    val finalists by viewModel.finalists.collectAsState()
    var surprise by remember { mutableStateOf<GameEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Pick your game", style = MaterialTheme.typography.displayMedium)
            Text(
                "One game. Full focus. No browsing.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search to commit…") },
                singleLine = true,
            )
        }
        if (results.isNotEmpty()) {
            item {
                Text("Results", style = MaterialTheme.typography.titleMedium)
            }
            items(results, key = { it.id }) { game ->
                SearchResultRow(game, viewModel, onClick = { onGameSelected(game.id) })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { viewModel.surpriseMe { surprise = it } }) {
                    Text("Surprise me")
                }
                OutlinedButton(onClick = { viewModel.pickFinalists() }) {
                    Text("Pick 3 finalists")
                }
            }
        }
        surprise?.let { game ->
            item {
                Text("Your surprise pick", style = MaterialTheme.typography.titleMedium)
                FinalistCard(game, viewModel, onClick = { onGameSelected(game.id) })
            }
        }
        if (finalists.isNotEmpty()) {
            item {
                Text("Choose a finalist", style = MaterialTheme.typography.titleMedium)
            }
            items(finalists, key = { it.id }) { game ->
                FinalistCard(game, viewModel, onClick = { onGameSelected(game.id) })
            }
        }
        if (shelf.isNotEmpty()) {
            item {
                Text("From your shelf", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shelf, key = { it.id }) { game ->
                        ShelfTile(game, viewModel, onClick = { onGameSelected(game.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    game: GameEntity,
    viewModel: PlayPickerViewModel,
    onClick: () -> Unit,
) {
    val media by viewModel.observeMedia(game.id).collectAsState(initial = emptyList())
    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameCoverImage(cover, Modifier.height(64.dp).aspectRatio(0.75f))
        Column(Modifier.weight(1f)) {
            Text(game.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(game.fileName, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FinalistCard(
    game: GameEntity,
    viewModel: PlayPickerViewModel,
    onClick: () -> Unit,
) {
    val media by viewModel.observeMedia(game.id).collectAsState(initial = emptyList())
    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        GameCoverImage(cover, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Spacer(Modifier.height(8.dp))
        Text(game.title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ShelfTile(
    game: GameEntity,
    viewModel: PlayPickerViewModel,
    onClick: () -> Unit,
) {
    val media by viewModel.observeMedia(game.id).collectAsState(initial = emptyList())
    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
    Column(
        Modifier
            .height(140.dp)
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
    ) {
        GameCoverImage(cover, Modifier.weight(1f).fillMaxWidth())
        Text(game.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}
