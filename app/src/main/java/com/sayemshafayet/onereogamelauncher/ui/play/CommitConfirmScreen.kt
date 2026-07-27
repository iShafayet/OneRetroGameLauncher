package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.CommitConfirmViewModel

@Composable
fun CommitConfirmScreen(
    onConfirmed: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CommitConfirmViewModel = hiltViewModel(),
) {
    val game by viewModel.game.collectAsState()
    val systemName by viewModel.systemDisplayName.collectAsState()
    val media by viewModel.media.collectAsState()
    val error by viewModel.error.collectAsState()
    val isCommitting by viewModel.isCommitting.collectAsState()
    val commitFocus = rememberOrlgFocusRequester()

    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
        ?: media.firstOrNull()?.path
    val isWide = rememberIsWidePlayLayout()

    PlayWideContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isWide) {
                CommitConfirmWideBody(
                    game = game,
                    systemName = systemName,
                    coverPath = cover,
                    slotIndex = viewModel.slotIndex,
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CommitConfirmHeader(slotIndex = viewModel.slotIndex)
                    CommitConfirmGameSection(
                        game = game,
                        systemName = systemName,
                        coverPath = cover,
                    )
                    CommitConfirmRulesPanel()
                }
            }

            CommitConfirmFooter(
                error = error,
                game = game,
                isCommitting = isCommitting,
                commitFocus = commitFocus,
                onCommit = { viewModel.commit(onConfirmed) },
                onCancel = onCancel,
            )
        }
    }
    OrlgInitialFocus(commitFocus, enabled = game != null && !isCommitting)
}

@Composable
private fun ColumnScope.CommitConfirmWideBody(
    game: GameEntity?,
    systemName: String?,
    coverPath: String?,
    slotIndex: Int,
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
                .weight(if (compact) 0.34f else 0.4f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (game == null) {
                CircularProgressIndicator()
            } else {
                GameCoverImage(
                    path = coverPath,
                    modifier = Modifier
                        .fillMaxHeight(if (compact) 0.98f else 0.9f)
                        .aspectRatio(0.72f)
                        .widthIn(max = if (compact) 200.dp else 280.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(if (compact) 0.66f else 0.6f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            CommitConfirmHeader(slotIndex = slotIndex, compact = compact)
            if (game != null) {
                Text(
                    game.title,
                    style = if (compact) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                systemName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CommitConfirmRulesPanel(compact = compact)
        }
    }
}

@Composable
private fun CommitConfirmHeader(slotIndex: Int, compact: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
        Text(
            "Confirm your pick",
            style = if (compact) {
                MaterialTheme.typography.headlineSmall.copy(fontFamily = BrandFont)
            } else {
                MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont)
            },
        )
        Text(
            if (compact) {
                "Lock this game into Play slot ${slotIndex + 1}. Finish or drop it before picking another here."
            } else {
                "You're about to lock in this game for Play slot ${slotIndex + 1}. " +
                    "Other slots are unaffected — finish or drop this one before picking a replacement here."
            },
            style = if (compact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommitConfirmGameSection(
    game: GameEntity?,
    systemName: String?,
    coverPath: String?,
) {
    if (game == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                "Loading game…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        SelectedGameCard(
            title = game.title,
            systemName = systemName,
            coverPath = coverPath,
        )
    }
}

@Composable
private fun CommitConfirmRulesPanel(compact: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 10.dp else 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        ) {
            Text(
                "What happens next",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LockRuleLine("Play mode stays on this game only")
            LockRuleLine("Finish or drop it to pick again")
            LockRuleLine("Setup mode stays available anytime")
        }
    }
}

@Composable
private fun CommitConfirmFooter(
    error: String?,
    game: GameEntity?,
    isCommitting: Boolean,
    commitFocus: androidx.compose.ui.focus.FocusRequester,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = onCommit,
                enabled = game != null && !isCommitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .focusRequester(commitFocus)
                    .then(if (game != null && !isCommitting) PulseModifier(true) else Modifier),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                if (isCommitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                } else {
                    Text("Confirm selection")
                }
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !isCommitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Choose another game")
            }
        }
    }
}

@Composable
private fun SelectedGameCard(
    title: String,
    systemName: String?,
    coverPath: String?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameCoverImage(
                path = coverPath,
                modifier = Modifier
                    .width(88.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Selected game",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                systemName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun LockRuleLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
