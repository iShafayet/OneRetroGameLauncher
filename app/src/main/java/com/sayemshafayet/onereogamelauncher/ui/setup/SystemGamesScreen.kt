package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.data.prefs.GameListLayout
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.domain.VirtualLibrarySystem
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.SearchField
import com.sayemshafayet.onereogamelauncher.ui.input.OrglFilterChip
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.FocusRing
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.GameListFilter
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SystemGamesViewModel

@Composable
fun SystemGamesScreen(
    onGameClick: (Long) -> Unit,
    viewModel: SystemGamesViewModel = hiltViewModel(),
) {
    val games by viewModel.games.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.gameFilter.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val firstFocus = rememberOrlgFocusRequester()
    val hideFavoriteFilter = VirtualLibrarySystem.fromId(viewModel.systemId) == VirtualLibrarySystem.FAVORITES
    val hideWishlistFilter = VirtualLibrarySystem.fromId(viewModel.systemId) == VirtualLibrarySystem.WISHLIST
    val visibleFilters = GameListFilter.entries.filter { f ->
        when (f) {
            GameListFilter.FAVORITE -> !hideFavoriteFilter
            GameListFilter.WISHLIST -> !hideWishlistFilter
            else -> true
        }
    }

    Column(Modifier.fillMaxSize()) {
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
            visibleFilters.forEach { f ->
                OrglFilterChip(
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
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
                        GameGridTile(
                            game = game,
                            observeMedia = { viewModel.observeMedia(game.id) },
                            onClick = { onGameClick(game.id) },
                            modifier = Modifier.orlgListFocus(index, firstFocus),
                        )
                    }
                }
            }
            GameListLayout.LIST -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
                        GameListRow(
                            game = game,
                            observeMedia = { viewModel.observeMedia(game.id) },
                            onClick = { onGameClick(game.id) },
                            modifier = Modifier
                                .orlgListFocus(index, firstFocus)
                                .orlgFocusable(onClick = { onGameClick(game.id) }),
                        )
                    }
                }
            }
        }
        OrlgInitialFocus(firstFocus, enabled = games.isNotEmpty())
    }
}

@Composable
private fun GameGridTile(
    game: GameEntity,
    observeMedia: () -> kotlinx.coroutines.flow.Flow<List<com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val media by observeMedia().collectAsState(initial = emptyList())
    val cover = coverPath(media)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val coverShape = RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "gridFocusScale",
    )
    val accent = FocusRing.copy(alpha = 0.85f)
    val frameLight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

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
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .shadow(
                    elevation = if (focused) 8.dp else 3.dp,
                    shape = coverShape,
                    clip = false,
                )
                .clip(coverShape),
        ) {
            GameCoverImage(
                path = cover,
                modifier = Modifier.fillMaxSize(),
            )
            if (focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(width = 2.dp, color = accent, shape = coverShape)
                        .padding(2.dp)
                        .border(width = 1.dp, color = frameLight, shape = RoundedCornerShape(10.dp)),
                )
            }
        }
        Text(
            game.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (focused) 1f else 0.88f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun GameListRow(
    game: GameEntity,
    observeMedia: () -> kotlinx.coroutines.flow.Flow<List<com.sayemshafayet.onereogamelauncher.data.db.entity.MediaEntity>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier.fillMaxWidth(),
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
        GameListFilter.WISHLIST -> "Wish"
        GameListFilter.FINISHED -> "Done"
        GameListFilter.DROPPED -> "Dropped"
    }
