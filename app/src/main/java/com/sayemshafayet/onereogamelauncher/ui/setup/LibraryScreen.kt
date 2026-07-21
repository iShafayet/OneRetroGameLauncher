package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SystemWithCount

@Composable
fun LibraryScreen(
    onSystemClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val systems by viewModel.visibleSystems.collectAsState()
    val totalGames by viewModel.totalGames.collectAsState()
    val firstFocus = rememberOrlgFocusRequester()

    Column(Modifier.fillMaxSize()) {
        Text(
            if (systems.isEmpty()) {
                "No games found — set your ROMs folder in Settings → Folders, then rescan."
            } else {
                "${systems.size} systems · $totalGames games"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(systems, key = { _, row -> row.system.id }) { index, row ->
                SystemRow(
                    row,
                    modifier = Modifier
                        .orlgListFocus(index, firstFocus)
                        .orlgFocusable(onClick = { onSystemClick(row.system.id) }),
                )
            }
        }
    }
    OrlgInitialFocus(firstFocus, enabled = systems.isNotEmpty())
}

@Composable
private fun SystemRow(
    row: SystemWithCount,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(row.system.displayName) },
        supportingContent = {
            Text("${row.system.folderName} · ${row.gameCount} games")
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
        modifier = modifier.fillMaxWidth(),
    )
    HorizontalDivider()
}
