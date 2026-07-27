package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.util.formatActivityLabel
import com.sayemshafayet.onereogamelauncher.ui.util.starsLabel
import com.sayemshafayet.onereogamelauncher.play.PlayCompletionData
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayCompletionViewModel
import java.io.File

@Composable
fun PlayCompletionScreen(
    onStartNewAdventure: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onMissingData: () -> Unit,
    viewModel: PlayCompletionViewModel = hiltViewModel(),
) {
    val completion by viewModel.completion.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isHistoryView = viewModel.isHistoryView
    val isSaving by viewModel.isSaving.collectAsState()
    val savedToGallery by viewModel.savedToGallery.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val newAdventureFocus = rememberOrlgFocusRequester()
    val backFocus = rememberOrlgFocusRequester()

    LaunchedEffect(isLoading, completion) {
        if (!isLoading && completion == null) onMissingData()
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (completion == null) return

    val data = completion!!

    val finished = data.status == CommitmentStatus.FINISHED
    val headline = when {
        isHistoryView && finished -> "Finished run"
        isHistoryView && !finished -> "Dropped run"
        finished -> "Run complete!"
        else -> "Run ended"
    }
    val subtitle = when {
        isHistoryView -> "${data.gameTitle} · ${data.systemName}"
        finished -> "You finished ${data.gameTitle}. Nice work — that's what Play mode is for."
        else -> "You dropped ${data.gameTitle}. No shame — every run teaches you something."
    }
    val isWide = rememberIsWidePlayLayout()

    PlayWideContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                PlayCompletionWideBody(
                    headline = headline,
                    subtitle = subtitle,
                    data = data,
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        headline,
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PlayCompletionRunCard(data = data)
                }
            }

            PlayCompletionFooter(
                data = data,
                saveMessage = saveMessage,
                savedToGallery = savedToGallery,
                isSaving = isSaving,
                isHistoryView = isHistoryView,
                onBack = onBack,
                onStartNewAdventure = onStartNewAdventure,
                newAdventureFocus = newAdventureFocus,
                backFocus = backFocus,
                onClearCompletion = viewModel::clearCompletion,
                onSaveToGallery = viewModel::saveToGallery,
            )
        }
    }

    OrlgInitialFocus(
        if (isHistoryView) backFocus else newAdventureFocus,
        enabled = completion != null,
    )
}

@Composable
private fun ColumnScope.PlayCompletionWideBody(
    headline: String,
    subtitle: String,
    data: PlayCompletionData,
) {
    val compact = rememberIsCompactWidePlayLayout()
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 12.dp else 24.dp,
                vertical = if (compact) 8.dp else 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(if (compact) 0.38f else 0.42f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            PlayCompletionCollage(data = data, fillHeight = true, compact = compact)
        }
        Column(
            modifier = Modifier
                .weight(if (compact) 0.62f else 0.58f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            Text(
                headline,
                style = if (compact) {
                    MaterialTheme.typography.headlineSmall.copy(fontFamily = BrandFont)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont)
                },
            )
            Text(
                subtitle,
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlayCompletionDetails(data = data)
        }
    }
}

@Composable
private fun PlayCompletionRunCard(data: PlayCompletionData) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayCompletionCollage(data = data, fillHeight = false)
            PlayCompletionDetails(data = data)
        }
    }
}

@Composable
private fun PlayCompletionCollage(
    data: PlayCompletionData,
    fillHeight: Boolean,
    compact: Boolean = false,
) {
    val collageModifier = if (fillHeight) {
        Modifier
            .fillMaxHeight(if (compact) 0.98f else 0.95f)
            .aspectRatio(1080f / 1350f)
            .widthIn(max = if (compact) 260.dp else 360.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(1080f / 1350f)
    }
    data.collagePath?.let { collagePath ->
        RunCardPreview(path = collagePath, modifier = collageModifier)
    } ?: run {
        Surface(
            modifier = collageModifier,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    data.gameTitle,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    data.systemName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlayCompletionDetails(data: PlayCompletionData) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            data.gameTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            data.systemName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            starsLabel(data.stars),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            formatActivityLabel(data.playtimeMs, data.sessionCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        data.reviewExcerpt?.takeIf { it.isNotBlank() }?.let {
            Text(
                "\"$it\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlayCompletionFooter(
    data: PlayCompletionData,
    saveMessage: String?,
    savedToGallery: Boolean,
    isSaving: Boolean,
    isHistoryView: Boolean,
    onBack: (() -> Unit)?,
    onStartNewAdventure: (() -> Unit)?,
    newAdventureFocus: androidx.compose.ui.focus.FocusRequester,
    backFocus: androidx.compose.ui.focus.FocusRequester,
    onClearCompletion: () -> Unit,
    onSaveToGallery: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            saveMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (savedToGallery) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedButton(
                onClick = onSaveToGallery,
                enabled = data.collagePath != null && !isSaving && !savedToGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                when {
                    isSaving -> CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                    )
                    savedToGallery -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Saved to gallery")
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save run card to gallery")
                    }
                }
            }
            if (isHistoryView && onBack != null) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .focusRequester(backFocus),
                ) {
                    Text("Back to history")
                }
            } else if (onStartNewAdventure != null) {
                Button(
                    onClick = {
                        onClearCompletion()
                        onStartNewAdventure()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .focusRequester(newAdventureFocus)
                        .then(PulseModifier(true)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text("Start a new adventure")
                }
            }
        }
    }
}

@Composable
private fun RunCardPreview(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = remember(path) {
        ImageRequest.Builder(context)
            .data(File(path))
            .crossfade(true)
            .build()
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AsyncImage(
            model = model,
            contentDescription = "Run card",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}
