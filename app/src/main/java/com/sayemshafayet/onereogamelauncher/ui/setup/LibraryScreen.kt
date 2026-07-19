package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.components.SearchField
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryFilter
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SystemWithCount

@Composable
fun LibraryScreen(
    onSystemClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val systems by viewModel.visibleSystems.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.libraryFilter.collectAsState()
    val totalGames by viewModel.totalGames.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SearchField(
            value = query,
            onValueChange = viewModel::setQuery,
            placeholder = "Search systems",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            if (systems.isEmpty()) {
                "No games found — pick your ROMs folder in Settings → ES-DE / Library, then rescan."
            } else {
                "${systems.size} systems · $totalGames games"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f.label) },
                )
            }
        }
        if (filter != LibraryFilter.ALL) {
            Text(
                "Open a system to filter games by ${filter.label.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(systems, key = { it.system.id }) { row ->
                SystemRow(row, onClick = { onSystemClick(row.system.id) })
            }
        }
    }
}

@Composable
private fun SystemRow(row: SystemWithCount, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(row.system.displayName) },
        supportingContent = {
            Text("${row.system.folderName} · ${row.gameCount} games")
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

private val LibraryFilter.label: String
    get() = when (this) {
        LibraryFilter.ALL -> "All"
        LibraryFilter.FAVORITE -> "Favorites"
        LibraryFilter.UNPLAYED -> "Unplayed"
        LibraryFilter.FINISHED -> "Finished"
        LibraryFilter.DROPPED -> "Dropped"
    }
