package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.PlayCompletionViewModel
import java.io.File

@Composable
fun PlayCompletionScreen(
    onStartNewAdventure: () -> Unit,
    onMissingData: () -> Unit,
    viewModel: PlayCompletionViewModel = hiltViewModel(),
) {
    val completion = viewModel.completion
    val isSaving by viewModel.isSaving.collectAsState()
    val savedToGallery by viewModel.savedToGallery.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val newAdventureFocus = rememberOrlgFocusRequester()

    LaunchedEffect(completion) {
        if (completion == null) onMissingData()
    }

    if (completion == null) return

    val finished = completion.status == CommitmentStatus.FINISHED
    val headline = if (finished) "Run complete!" else "Run ended"
    val subtitle = if (finished) {
        "You finished ${completion.gameTitle}. Nice work — that's what Play mode is for."
    } else {
        "You dropped ${completion.gameTitle}. No shame — every run teaches you something."
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    completion.collagePath?.let { collagePath ->
                        RunCardPreview(
                            path = collagePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1080f / 1350f),
                        )
                    } ?: run {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1080f / 1350f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    completion.gameTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    completion.systemName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            completion.gameTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            completion.systemName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            starsLabel(completion.stars),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            formatActivityLabel(completion.playtimeMs, completion.sessionCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        completion.reviewExcerpt?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                "\"$it\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

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
                    onClick = viewModel::saveToGallery,
                    enabled = completion.collagePath != null && !isSaving && !savedToGallery,
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
                Button(
                    onClick = {
                        viewModel.clearCompletion()
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

    OrlgInitialFocus(newAdventureFocus)
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
