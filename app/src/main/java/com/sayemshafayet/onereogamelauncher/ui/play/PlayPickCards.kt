package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.theme.LocalOrglPalette
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayPickerViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlaySuggestionBadge

internal val pickCardWidth = 132.dp
internal val pickCardWidthCompact = 112.dp
internal val pickRowHeight = 228.dp

/** Horizontal strip — portrait / narrow layouts. */
@Composable
internal fun PickCardRow(
    picks: List<PlayGamePick>,
    viewModel: PlayPickerViewModel,
    onGameSelected: (Long) -> Unit,
    firstFocus: FocusRequester,
    focusStartIndex: Int,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = pickRowHeight),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(picks, key = { _, pick -> pick.game.id }) { index, pick ->
            PlayPickCard(
                pick = pick,
                viewModel = viewModel,
                modifier = Modifier
                    .width(pickCardWidth)
                    .orlgListFocus(focusStartIndex + index, firstFocus)
                    .orlgFocusable(
                        onClick = { onGameSelected(pick.game.id) },
                        gamepadXActivates = true,
                    ),
            )
        }
    }
}

/**
 * Wrapping grid of pick cards. Non-lazy so it works inside LazyColumn / short Columns
 * without unbounded-height LazyVerticalGrid crashes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PickCardFlow(
    picks: List<PlayGamePick>,
    viewModel: PlayPickerViewModel,
    onGameSelected: (Long) -> Unit,
    firstFocus: FocusRequester,
    focusStartIndex: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val cardWidth = if (compact) pickCardWidthCompact else pickCardWidth
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        picks.forEachIndexed { index, pick ->
            PlayPickCard(
                pick = pick,
                viewModel = viewModel,
                compact = compact,
                modifier = Modifier
                    .width(cardWidth)
                    .orlgListFocus(focusStartIndex + index, firstFocus)
                    .orlgFocusable(
                        onClick = { onGameSelected(pick.game.id) },
                        gamepadXActivates = true,
                    ),
            )
        }
    }
}

@Composable
internal fun PlayPickCard(
    pick: PlayGamePick,
    viewModel: PlayPickerViewModel,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    compact: Boolean = false,
) {
    val media by viewModel.observeMedia(pick.game.id).collectAsState(initial = emptyList())
    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
        ?: media.firstOrNull { it.type == MediaType.SCREENSHOT }?.path
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    val borderColor = if (highlighted) {
        LocalOrglPalette.current.accent
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val badgeLabel = when (pick.badge) {
        PlaySuggestionBadge.WISHLIST -> "Wishlist"
        PlaySuggestionBadge.SUGGESTED -> "Suggested"
        null -> null
    }
    val pad = if (compact) 6.dp else 8.dp

    Card(
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .then(
                if (highlighted) {
                    Modifier.background(LocalOrglPalette.current.accent.copy(alpha = 0.08f), shape)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 6.dp else 2.dp),
    ) {
        Column(Modifier.padding(pad)) {
            GameCoverImage(
                path = cover,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp)),
            )
            Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
            Text(
                pick.game.title,
                style = if (compact) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.titleSmall
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
            Text(
                pick.systemDisplayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact) {
                badgeLabel?.let { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pick.badge == PlaySuggestionBadge.WISHLIST) {
                            LocalOrglPalette.current.accent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
