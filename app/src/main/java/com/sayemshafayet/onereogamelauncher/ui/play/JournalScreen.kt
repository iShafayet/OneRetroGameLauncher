package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.util.formatDate
import com.sayemshafayet.onereogamelauncher.ui.util.starsLabel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.JournalViewModel

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Journal", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                "Finished and dropped games appear here with your reviews.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries, key = { it.commitmentId }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    GameCoverImage(
                        path = entry.collagePath,
                        modifier = Modifier
                            .height(80.dp)
                            .width(64.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.gameTitle, style = MaterialTheme.typography.titleMedium)
                        Text(entry.systemDisplayName, style = MaterialTheme.typography.bodySmall)
                        Text(
                            when (entry.status) {
                                CommitmentStatus.FINISHED -> "Finished"
                                CommitmentStatus.DROPPED -> "Dropped"
                                else -> entry.status.name
                            } + " · " + formatDate(entry.releasedAt),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        entry.stars?.let {
                            Text(starsLabel(it), style = MaterialTheme.typography.bodyMedium)
                        }
                        entry.reviewText?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
