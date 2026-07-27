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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.data.prefs.resolveLibrarySystemsLayout
import com.sayemshafayet.onereogamelauncher.ui.components.SystemIconResolver
import com.sayemshafayet.onereogamelauncher.ui.util.rememberPhysicalLandscapeAspectRatio
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
    val landscapeAspectRatio = rememberPhysicalLandscapeAspectRatio()
    val layout = resolveLibrarySystemsLayout(
        preference = layoutPreference,
        orientation = configuration.orientation,
        landscapeAspectRatio = landscapeAspectRatio,
    )
    val firstFocus = rememberOrlgFocusRequester()
    val physicalCount = systems.count { it is LibrarySystemRow.Physical }
    val wideLandscape = rememberIsLibraryWideLandscape()
    val statsLine = if (physicalCount == 0 && systems.none { it is LibrarySystemRow.Virtual && it.gameCount > 0 }) {
        "No games found — set your ROMs folder in Settings → Folders, then rescan."
    } else {
        "$physicalCount systems · $totalGames games"
    }

    Column(Modifier.fillMaxSize()) {
        if (!wideLandscape) {
            Text(
                statsLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        when (layout) {
            GameListLayout.GRID -> {
                LazyVerticalGrid(
                    columns = if (wideLandscape) {
                        GridCells.Fixed(6)
                    } else {
                        GridCells.Adaptive(140.dp)
                    },
                    contentPadding = if (wideLandscape) {
                        PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    } else {
                        PaddingValues(14.dp)
                    },
                    horizontalArrangement = Arrangement.spacedBy(if (wideLandscape) 6.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (wideLandscape) 8.dp else 14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(systems, key = { _, row -> row.id }) { index, row ->
                        SystemGridTile(
                            row = row,
                            onClick = { onSystemClick(row.id) },
                            modifier = Modifier.orlgListFocus(index, firstFocus),
                            compact = wideLandscape,
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
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val tileShape = RoundedCornerShape(if (compact) 8.dp else 12.dp)
    val scale by animateFloatAsState(
        targetValue = if (focused) if (compact) 1.02f else 1.03f else 1f,
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
    val folderName = (row as? LibrarySystemRow.Physical)?.row?.system?.folderName
    val hasIconMapping = folderName != null &&
        SystemIconResolver.iconFileForFolder(folderName) != null
    val iconModel = remember(folderName, hasIconMapping) {
        if (!hasIconMapping) {
            null
        } else {
            SystemIconResolver.assetPathForFolder(folderName!!)?.let { path ->
                ImageRequest.Builder(context)
                    .data(path)
                    .crossfade(true)
                    .build()
            }
        }
    }
    // Theme-aware tile plate — icons are transparent; bottom ~40% is reserved for the name.
    val tileBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val onTile = MaterialTheme.colorScheme.onSurface
    val showNameOverlay = hasIconMapping
    val showNameInTile = !hasIconMapping

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
                .background(tileBackground)
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = borderColor,
                    shape = tileShape,
                ),
        ) {
            if (iconModel != null) {
                AsyncImage(
                    model = iconModel,
                    contentDescription = row.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (showNameOverlay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(if (compact) 0.38f else 0.40f)
                        .padding(
                            horizontal = if (compact) 4.dp else 8.dp,
                            vertical = if (compact) 3.dp else 6.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        row.displayName,
                        style = if (compact) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = onTile,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else if (showNameInTile) {
                Text(
                    row.displayName,
                    style = if (compact) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = onTile,
                    maxLines = if (compact) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(if (compact) 6.dp else 12.dp),
                )
            }
        }
        if (!compact) {
            Text(
                "${row.gameCount} games",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}
