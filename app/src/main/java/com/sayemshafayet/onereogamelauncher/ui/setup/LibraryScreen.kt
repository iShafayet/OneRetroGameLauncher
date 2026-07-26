package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.data.prefs.resolveLibrarySystemsLayout
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.FocusRing
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibrarySystemRow
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    onSystemClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val systems by viewModel.visibleSystems.collectAsState()
    val totalGames by viewModel.totalGames.collectAsState()
    val layoutPreference by viewModel.systemsLayoutPreference.collectAsState()
    val configuration = LocalConfiguration.current
    val layout = resolveLibrarySystemsLayout(
        preference = layoutPreference,
        orientation = configuration.orientation,
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
    val firstFocus = rememberOrlgFocusRequester()
    val physicalCount = systems.count { it is LibrarySystemRow.Physical }

    Column(Modifier.fillMaxSize()) {
        Text(
            if (physicalCount == 0 && systems.none { it is LibrarySystemRow.Virtual && it.gameCount > 0 }) {
                "No games found — set your ROMs folder in Settings → Folders, then rescan."
            } else {
                "$physicalCount systems · $totalGames games"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        when (layout) {
            GameListLayout.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(systems, key = { _, row -> row.id }) { index, row ->
                        SystemGridTile(
                            row = row,
                            onClick = { onSystemClick(row.id) },
                            modifier = Modifier.orlgListFocus(index, firstFocus),
                        )
                    }
                }
            }
            GameListLayout.LIST -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(systems, key = { _, row -> row.id }) { index, row ->
                        SystemListRow(
                            row = row,
                            modifier = Modifier
                                .orlgListFocus(index, firstFocus)
                                .orlgFocusable(onClick = { onSystemClick(row.id) }),
                        )
                    }
                }
            }
        }
    }
    OrlgInitialFocus(firstFocus, enabled = systems.isNotEmpty())
}

@Composable
private fun SystemListRow(
    row: LibrarySystemRow,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(row.displayName) },
        supportingContent = {
            Text(
                when (row) {
                    is LibrarySystemRow.Virtual -> "${row.subtitle} · ${row.gameCount} games"
                    is LibrarySystemRow.Physical -> row.subtitle
                },
            )
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
        modifier = modifier.fillMaxWidth(),
    )
    HorizontalDivider()
}

@Composable
private fun SystemGridTile(
    row: LibrarySystemRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val tileShape = RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "systemGridFocusScale",
    )
    val accent = FocusRing.copy(alpha = 0.85f)
    val borderColor = if (focused) {
        accent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val supporting = when (row) {
        is LibrarySystemRow.Virtual -> "${row.gameCount} games"
        is LibrarySystemRow.Physical -> "${row.gameCount} games"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .orlgFocusable(
                onClick = onClick,
                showFocusRing = false,
                interactionSource = interactionSource,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(tileShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = borderColor,
                    shape = tileShape,
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            supporting,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (focused) 1f else 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}
