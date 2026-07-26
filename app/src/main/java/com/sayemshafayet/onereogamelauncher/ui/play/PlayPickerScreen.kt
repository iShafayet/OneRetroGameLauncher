package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.components.SearchField
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintOverlay
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.input.rememberShowGamepadHints
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayPickerPhase
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayPickerViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlaySuggestionBadge

private val pickCardWidth = 132.dp
private val pickRowHeight = 228.dp

@Composable
fun PlayPickerScreen(
    onGameSelected: (Long) -> Unit,
    viewModel: PlayPickerViewModel = hiltViewModel(),
) {
    val phase by viewModel.phase.collectAsState()
    val isLoadingPicker by viewModel.isLoadingPicker.collectAsState()

    when (phase) {
        PlayPickerPhase.INTRO -> PlayIntroContent(
            onContinue = viewModel::enterPicker,
            isLoading = isLoadingPicker,
        )
        PlayPickerPhase.PICKING -> PlayPickerContent(
            onGameSelected = onGameSelected,
            onBackToIntro = viewModel::backToIntro,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun PlayIntroContent(
    onContinue: () -> Unit,
    isLoading: Boolean,
) {
    val scrollState = rememberScrollState()
    val continueFocus = rememberOrlgFocusRequester()
    val continueInteraction = remember { MutableInteractionSource() }
    val continueFocused by continueInteraction.collectIsFocusedAsState()
    val showHints = rememberShowGamepadHints()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                "Play mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                "One game.\nFull focus.",
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = BrandFont),
            )
            Text(
                "ORGL is built around commitment — not endless browsing. " +
                    "Pick a single game, give it a fair run, and don't move on until you're done with it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CommitRuleRow(number = "1", title = "Pick one game", body = "Choose from suggestions or search your library.")
                CommitRuleRow(number = "2", title = "Commit", body = "That game locks in — no switching until you release it.")
                CommitRuleRow(number = "3", title = "Finish or drop", body = "Mark it done or move on. Then you can pick again.")
            }
        }

        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onContinue,
                enabled = !isLoading,
                interactionSource = continueInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(continueFocus)
                    .then(if (!isLoading) PulseModifier(true) else Modifier),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                } else {
                    Text("Let's pick a game to play")
                }
            }
            if (showHints && continueFocused && !isLoading) {
                GamepadHintOverlay(
                    label = "A",
                    modifier = Modifier.align(Alignment.TopEnd),
                    offsetX = (-6).dp,
                    offsetY = (-6).dp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    OrlgInitialFocus(continueFocus, enabled = !isLoading)
}

@Composable
private fun CommitRuleRow(number: String, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            modifier = Modifier.width(36.dp),
        ) {
            Text(
                number,
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = BrandFont,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayPickerContent(
    onGameSelected: (Long) -> Unit,
    onBackToIntro: () -> Unit,
    viewModel: PlayPickerViewModel,
) {
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val shelf by viewModel.shelf.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val libraryEmpty by viewModel.libraryEmpty.collectAsState()
    val firstFocus = rememberOrlgFocusRequester()
    val searching = query.trim().length >= 2
    val hasPickTargets = suggestions.isNotEmpty() || shelf.isNotEmpty() ||
        (searching && searchResults.isNotEmpty())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PlayPickerHeader(onBackToIntro = onBackToIntro)
        }

        if (libraryEmpty) {
            item {
                Text(
                    "Scan your ROM folders in Setup first — then come back to commit to a game.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                SearchField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Search your library…",
                )
            }

            if (searching) {
                if (searchResults.isEmpty()) {
                    item {
                        Text(
                            "No matches — try another title or clear search to see suggestions.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item {
                        SectionLabel("Search results")
                    }
                    item {
                        PickCardRow(
                            picks = searchResults,
                            viewModel = viewModel,
                            onGameSelected = onGameSelected,
                            firstFocus = firstFocus,
                            focusStartIndex = 0,
                        )
                    }
                }
            } else {
                item {
                    SectionLabel(
                        "Tonight's trio",
                        subtitle = "Wishlist picks first when available; the rest are suggested at random.",
                    )
                }
                item {
                    PickCardRow(
                        picks = suggestions,
                        viewModel = viewModel,
                        onGameSelected = onGameSelected,
                        firstFocus = firstFocus,
                        focusStartIndex = 0,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::refreshSuggestions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle trio")
                    }
                }
            }

            if (!searching && shelf.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SectionLabel("Your shelf", subtitle = "Shortlisted games — max 5 in Setup.")
                }
                item {
                    PickCardRow(
                        picks = shelf,
                        viewModel = viewModel,
                        onGameSelected = onGameSelected,
                        firstFocus = firstFocus,
                        focusStartIndex = suggestions.size,
                    )
                }
            }
        }
    }
    OrlgInitialFocus(firstFocus, enabled = hasPickTargets)
}

@Composable
private fun PickCardRow(
    picks: List<PlayGamePick>,
    viewModel: PlayPickerViewModel,
    onGameSelected: (Long) -> Unit,
    firstFocus: androidx.compose.ui.focus.FocusRequester,
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
                    .orlgFocusable(onClick = { onGameSelected(pick.game.id) }),
            )
        }
    }
}

@Composable
private fun PlayPickerHeader(onBackToIntro: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Choose your game",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
            )
            TextButton(onClick = onBackToIntro) {
                Text("How it works")
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ) {
            Text(
                "One game locked in until you finish or drop it — then you can pick again.",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayPickCard(
    pick: PlayGamePick,
    viewModel: PlayPickerViewModel,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val media by viewModel.observeMedia(pick.game.id).collectAsState(initial = emptyList())
    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
        ?: media.firstOrNull { it.type == MediaType.SCREENSHOT }?.path
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (highlighted) AmberAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val badgeLabel = when (pick.badge) {
        PlaySuggestionBadge.WISHLIST -> "Wishlist"
        PlaySuggestionBadge.SUGGESTED -> "Suggested"
        null -> null
    }

    Card(
        modifier = modifier
            .border(1.dp, borderColor, shape)
            .then(
                if (highlighted) {
                    Modifier.background(AmberAccent.copy(alpha = 0.08f), shape)
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
        Column(Modifier.padding(8.dp)) {
            GameCoverImage(
                path = cover,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                pick.game.title,
                style = MaterialTheme.typography.titleSmall,
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
            badgeLabel?.let { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pick.badge == PlaySuggestionBadge.WISHLIST) {
                        AmberAccent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
