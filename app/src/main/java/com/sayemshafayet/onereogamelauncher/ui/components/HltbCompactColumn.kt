package com.sayemshafayet.onereogamelauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.domain.HltbUiPhase
import com.sayemshafayet.onereogamelauncher.domain.RaButtonState
import com.sayemshafayet.onereogamelauncher.ui.util.formatHltbCompactHours

/** Fixed width for `HLTB` / `999h` plus a little padding — must not grow with the RA row. */
val HltbCompactColumnWidth = 52.dp

@Composable
fun HltbCompactColumn(
    phase: HltbUiPhase,
    mainHours: Double?,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    if (!visible) return
    Column(
        modifier = modifier
            .width(HltbCompactColumnWidth)
            .padding(start = 4.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "HLTB",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            when (phase) {
                HltbUiPhase.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HltbUiPhase.Error -> {
                    Text(
                        "x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                HltbUiPhase.Missing -> {
                    Text(
                        "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                HltbUiPhase.Ready -> {
                    Text(
                        formatHltbCompactHours(mainHours),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
fun RaWithHltbRow(
    raState: RaButtonState,
    raLoading: Boolean,
    onOpenRetroAchievements: () -> Unit,
    hltbPhase: HltbUiPhase,
    hltbMainHours: Double?,
    hltbVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RetroAchievementsButton(
            state = raState,
            loading = raLoading,
            onClick = onOpenRetroAchievements,
            modifier = Modifier.weight(1f),
        )
        HltbCompactColumn(
            phase = hltbPhase,
            mainHours = hltbMainHours,
            visible = hltbVisible,
        )
    }
}
