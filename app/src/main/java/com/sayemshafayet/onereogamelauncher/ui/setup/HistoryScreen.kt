package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.util.formatActivityLabel
import com.sayemshafayet.onereogamelauncher.ui.util.formatDate
import com.sayemshafayet.onereogamelauncher.ui.util.starsLabel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.HistoryEntryUi
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    onOpenRun: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val entries = ui.entries
    val firstFocus = rememberOrlgFocusRequester()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "History",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
        )
        Text(
            "Every game you finished or dropped in Play mode — with your ratings and reviews.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (ui.loading && entries.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        Text(
                            "Loading journal from disk…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (entries.isEmpty()) {
                item {
                    Text(
                        "No completed runs yet. Commit to a game in Play mode and finish or drop it — it will show up here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(entries, key = { _, item -> item.entry.commitmentId }) { index, item ->
                    HistoryEntryCard(
                        item = item,
                        index = index,
                        firstFocus = firstFocus,
                        onOpenRun = { onOpenRun(item.entry.commitmentId) },
                    )
                }
            }
            item {
                HistorySyncFooter(
                    orglConfigured = ui.orglConfigured,
                    syncing = ui.syncing,
                    syncMessage = ui.syncMessage,
                    onSync = viewModel::syncWithDisk,
                    focusIndex = entries.size,
                    firstFocus = firstFocus,
                )
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
    OrlgInitialFocus(firstFocus, enabled = entries.isNotEmpty() || ui.orglConfigured)
}

@Composable
private fun HistorySyncFooter(
    orglConfigured: Boolean,
    syncing: Boolean,
    syncMessage: String?,
    onSync: () -> Unit,
    focusIndex: Int,
    firstFocus: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Disk sync",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Merge your Play mode journal with play_history.json in the ORGL data folder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onSync,
            enabled = orglConfigured && !syncing,
            modifier = Modifier
                .fillMaxWidth()
                .orlgListFocus(focusIndex, firstFocus)
                .orlgFocusable(onClick = onSync),
        ) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Sync with disk")
            }
        }
        if (!orglConfigured) {
            Text(
                "Set an ORGL data folder in Settings to enable sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        syncMessage?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(
    item: HistoryEntryUi,
    index: Int,
    firstFocus: FocusRequester,
    onOpenRun: () -> Unit,
) {
    val entry = item.entry
    val cover = item.boxArtPath ?: entry.collagePath
    val statusLabel = when (entry.status) {
        CommitmentStatus.FINISHED -> "Finished"
        CommitmentStatus.DROPPED -> "Dropped"
        else -> entry.status.name
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                GameCoverImage(
                    path = cover,
                    modifier = Modifier
                        .width(88.dp)
                        .aspectRatio(0.72f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        entry.gameTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        entry.systemDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "$statusLabel · ${formatDate(entry.releasedAt ?: entry.committedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    entry.stars?.let {
                        Text(starsLabel(it), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        formatActivityLabel(entry.playtimeMs, entry.sessionCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    entry.reviewText?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onOpenRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .orlgListFocus(index, firstFocus)
                    .orlgFocusable(onClick = onOpenRun),
            ) {
                Text("View run card")
            }
        }
    }
}
