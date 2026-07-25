package com.sayemshafayet.onereogamelauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.domain.LibraryScanSummary
import com.sayemshafayet.onereogamelauncher.domain.SystemScanSummary

@Composable
fun LibraryScanSummaryPanel(
    summary: LibraryScanSummary,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Library summary",
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryStatLine("Systems with games", summary.systemsWithGames.toString(), labelColor, valueColor)
            SummaryStatLine("Games found", summary.gamesFound.toString(), labelColor, valueColor)
            SummaryStatLine("With metadata", summary.gamesWithMetadata.toString(), labelColor, valueColor)
            SummaryStatLine("With media", summary.gamesWithMedia.toString(), labelColor, valueColor)
            SummaryStatLine("Media files linked", summary.mediaLinked.toString(), labelColor, valueColor)
            if (summary.unknownFiles > 0) {
                SummaryStatLine(
                    "Unrecognized files",
                    summary.unknownFiles.toString(),
                    labelColor,
                    valueColor,
                )
            }
        }

        if (summary.systems.isEmpty()) {
            Text(
                "No matching ROM files were found under your ROMs folder.",
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
            )
        } else {
            Text(
                "By system",
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
            )
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                summary.systems.forEach { row ->
                    SystemSummaryRow(row, labelColor, valueColor)
                    HorizontalDivider(color = labelColor.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun SystemSummaryRow(
    row: SystemScanSummary,
    labelColor: Color,
    valueColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                row.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${row.gameCount}",
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
            )
        }
        Text(
            "${row.folderName} · ${row.withMetadata} metadata · ${row.withMedia} with media · ${row.mediaFiles} files",
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryStatLine(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
