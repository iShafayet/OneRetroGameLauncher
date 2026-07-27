package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.components.SearchField
import com.sayemshafayet.onereogamelauncher.ui.input.GamepadHintOverlay
import android.view.KeyEvent
import com.sayemshafayet.onereogamelauncher.ui.input.RegisterScreenButtonXHandler
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.input.rememberShowGamepadHints
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayPickerPhase
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayPickerViewModel

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
    val isWide = rememberIsWidePlayLayout()
    val compact = rememberIsCompactWidePlayLayout()
    val scrollState = rememberScrollState()
    val continueFocus = rememberOrlgFocusRequester()
    val continueInteraction = remember { MutableInteractionSource() }
    val showHints = rememberShowGamepadHints()
    val panePad = if (compact) 16.dp else 28.dp
    val paneGap = if (compact) 16.dp else 32.dp

    RegisterScreenButtonXHandler { event ->
        if (event.action != KeyEvent.ACTION_UP || isLoading) return@RegisterScreenButtonXHandler false
        onContinue()
        true
    }

    PlayWideContainer {
        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = panePad, vertical = if (compact) 12.dp else 20.dp),
                horizontalArrangement = Arrangement.spacedBy(paneGap),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlayIntroHero(compact = compact)
                    Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
                    PlayIntroContinueButton(
                        onContinue = onContinue,
                        isLoading = isLoading,
                        continueFocus = continueFocus,
                        continueInteraction = continueInteraction,
                        showHints = showHints,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PlayIntroRules(compact = compact)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                PlayIntroHero(compact = false)
                Spacer(Modifier.height(20.dp))
                PlayIntroRules(compact = false)
                Spacer(Modifier.height(24.dp))
                PlayIntroContinueButton(
                    onContinue = onContinue,
                    isLoading = isLoading,
                    continueFocus = continueFocus,
                    continueInteraction = continueInteraction,
                    showHints = showHints,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    OrlgInitialFocus(continueFocus, enabled = !isLoading)
}

@Composable
private fun PlayIntroHero(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 20.dp)) {
        Text(
            "Play mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            if (compact) "One game. Full focus." else "One game.\nFull focus.",
            style = if (compact) {
                MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont)
            } else {
                MaterialTheme.typography.displayMedium.copy(fontFamily = BrandFont)
            },
        )
        if (!compact) {
            Text(
                "ORGL is built around commitment — not endless browsing. " +
                    "Pick a single game, give it a fair run, and don't move on until you're done with it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayIntroRules(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
        CommitRuleRow(number = "1", title = "Pick one game", body = "Choose from suggestions or search your library.")
        CommitRuleRow(number = "2", title = "Commit", body = "That game locks in — no switching until you release it.")
        CommitRuleRow(number = "3", title = "Finish or drop", body = "Mark it done or move on. Then you can pick again.")
    }
}

@Composable
private fun PlayIntroContinueButton(
    onContinue: () -> Unit,
    isLoading: Boolean,
    continueFocus: androidx.compose.ui.focus.FocusRequester,
    continueInteraction: MutableInteractionSource,
    showHints: Boolean,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onContinue,
            enabled = !isLoading,
            interactionSource = continueInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(continueFocus),
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
        if (showHints && !isLoading) {
            GamepadHintOverlay(
                label = "X",
                modifier = Modifier.align(Alignment.TopEnd),
                offsetX = (-6).dp,
                offsetY = (-6).dp,
            )
        }
    }
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
    val isWide = rememberIsWidePlayLayout()
    val compact = rememberIsCompactWidePlayLayout()
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val shelf by viewModel.shelf.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val libraryEmpty by viewModel.libraryEmpty.collectAsState()
    val firstFocus = rememberOrlgFocusRequester()
    val searching = query.trim().length >= 2
    val hasPickTargets = suggestions.isNotEmpty() || shelf.isNotEmpty() ||
        (searching && searchResults.isNotEmpty())

    PlayWideContainer {
        if (isWide && !libraryEmpty) {
            PlayPickerWideContent(
                onGameSelected = onGameSelected,
                onBackToIntro = onBackToIntro,
                viewModel = viewModel,
                query = query,
                suggestions = suggestions,
                shelf = shelf,
                searchResults = searchResults,
                searching = searching,
                firstFocus = firstFocus,
                compact = compact,
            )
        } else {
            PlayPickerNarrowContent(
                onGameSelected = onGameSelected,
                onBackToIntro = onBackToIntro,
                viewModel = viewModel,
                query = query,
                suggestions = suggestions,
                shelf = shelf,
                searchResults = searchResults,
                libraryEmpty = libraryEmpty,
                searching = searching,
                firstFocus = firstFocus,
                useFlowGrid = false,
            )
        }
    }
    OrlgInitialFocus(firstFocus, enabled = hasPickTargets)
}

@Composable
private fun PlayPickerWideContent(
    onGameSelected: (Long) -> Unit,
    onBackToIntro: () -> Unit,
    viewModel: PlayPickerViewModel,
    query: String,
    suggestions: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    shelf: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    searchResults: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    searching: Boolean,
    firstFocus: androidx.compose.ui.focus.FocusRequester,
    compact: Boolean,
) {
    val hPad = if (compact) 12.dp else 20.dp
    val vPad = if (compact) 10.dp else 16.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPad, vertical = vPad),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(if (compact) 0.32f else 0.34f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            ) {
                TextButton(onClick = onBackToIntro) {
                    Text("How it works")
                }
                PlayPickerHeader(compact = true)
                SearchField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Search your library…",
                )
                if (!searching) {
                    OutlinedButton(
                        onClick = viewModel::refreshSuggestions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (compact) "Shuffle" else "Shuffle trio")
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(if (compact) 0.68f else 0.66f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
            ) {
                if (searching) {
                    if (searchResults.isEmpty()) {
                        Text(
                            "No matches — try another title or clear search to see suggestions.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        SectionLabel("Search results")
                        PickCardFlow(
                            picks = searchResults,
                            viewModel = viewModel,
                            onGameSelected = onGameSelected,
                            firstFocus = firstFocus,
                            focusStartIndex = 0,
                            compact = compact,
                        )
                    }
                } else {
                    SectionLabel(
                        "Tonight's trio",
                        subtitle = if (compact) null else "Wishlist first when available.",
                    )
                    PickCardFlow(
                        picks = suggestions,
                        viewModel = viewModel,
                        onGameSelected = onGameSelected,
                        firstFocus = firstFocus,
                        focusStartIndex = 0,
                        compact = compact,
                    )
                    if (shelf.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        SectionLabel(
                            "Your shelf",
                            subtitle = if (compact) null else "Max 5 in Setup.",
                        )
                        PickCardFlow(
                            picks = shelf,
                            viewModel = viewModel,
                            onGameSelected = onGameSelected,
                            firstFocus = firstFocus,
                            focusStartIndex = suggestions.size,
                            compact = compact,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayPickerNarrowContent(
    onGameSelected: (Long) -> Unit,
    onBackToIntro: () -> Unit,
    viewModel: PlayPickerViewModel,
    query: String,
    suggestions: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    shelf: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    searchResults: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    libraryEmpty: Boolean,
    searching: Boolean,
    firstFocus: androidx.compose.ui.focus.FocusRequester,
    useFlowGrid: Boolean,
) {
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
                    item { SectionLabel("Search results") }
                    item {
                        PlayPickCardsBlock(
                            picks = searchResults,
                            viewModel = viewModel,
                            onGameSelected = onGameSelected,
                            firstFocus = firstFocus,
                            focusStartIndex = 0,
                            useFlowGrid = useFlowGrid,
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
                    PlayPickCardsBlock(
                        picks = suggestions,
                        viewModel = viewModel,
                        onGameSelected = onGameSelected,
                        firstFocus = firstFocus,
                        focusStartIndex = 0,
                        useFlowGrid = useFlowGrid,
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
                    PlayPickCardsBlock(
                        picks = shelf,
                        viewModel = viewModel,
                        onGameSelected = onGameSelected,
                        firstFocus = firstFocus,
                        focusStartIndex = suggestions.size,
                        useFlowGrid = useFlowGrid,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayPickCardsBlock(
    picks: List<com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayGamePick>,
    viewModel: PlayPickerViewModel,
    onGameSelected: (Long) -> Unit,
    firstFocus: androidx.compose.ui.focus.FocusRequester,
    focusStartIndex: Int,
    useFlowGrid: Boolean,
) {
    if (useFlowGrid) {
        PickCardFlow(
            picks = picks,
            viewModel = viewModel,
            onGameSelected = onGameSelected,
            firstFocus = firstFocus,
            focusStartIndex = focusStartIndex,
        )
    } else {
        PickCardRow(
            picks = picks,
            viewModel = viewModel,
            onGameSelected = onGameSelected,
            firstFocus = firstFocus,
            focusStartIndex = focusStartIndex,
        )
    }
}

@Composable
private fun PlayPickerHeader(
    onBackToIntro: () -> Unit = {},
    compact: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (compact) {
            Text(
                "Choose your game",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
            )
        } else {
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
