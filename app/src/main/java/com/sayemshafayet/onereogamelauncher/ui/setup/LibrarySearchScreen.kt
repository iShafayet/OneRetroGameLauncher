package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.SearchField
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibrarySearchResult
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibrarySearchViewModel
import kotlinx.coroutines.delay

@Composable
fun LibrarySearchScreen(
    onGameClick: (Long) -> Unit,
    viewModel: LibrarySearchViewModel = hiltViewModel(),
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.results.collectAsState()
    val searchFocus = remember { FocusRequester() }
    val firstResultFocus = rememberOrlgFocusRequester()

    LaunchedEffect(Unit) {
        delay(50)
        runCatching { searchFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = "Search all games",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(searchFocus),
        )

        when {
            query.isBlank() -> {
                Text(
                    "Search across every system in your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            results.isEmpty() -> {
                Text(
                    "No games match “$query”.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            else -> {
                Text(
                    "${results.size} result${if (results.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(results, key = { _, row -> row.game.id }) { index, row ->
                        LibrarySearchRow(
                            row = row,
                            observeMedia = { viewModel.observeMedia(row.game.id) },
                            modifier = Modifier
                                .orlgListFocus(index, firstResultFocus)
                                .orlgFocusable(onClick = { onGameClick(row.game.id) }),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchRow(
    row: LibrarySearchResult,
    observeMedia: () -> kotlinx.coroutines.flow.Flow<List<com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity>>,
    modifier: Modifier = Modifier,
) {
    val media by observeMedia().collectAsState(initial = emptyList())
    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
        ?: media.firstOrNull { it.type == MediaType.SCREENSHOT }?.path
        ?: media.firstOrNull()?.path
    val systemLabel = when {
        row.systemDisplayName.isNotBlank() && row.systemFolder.isNotBlank() ->
            "${row.systemDisplayName} · ${row.systemFolder}"
        row.systemDisplayName.isNotBlank() -> row.systemDisplayName
        else -> row.systemFolder
    }

    ListItem(
        headlineContent = {
            Text(row.game.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                if (systemLabel.isNotBlank()) systemLabel else row.game.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        modifier = modifier.fillMaxWidth(),
    )
    HorizontalDivider()
}
