package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.ScanStage
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryScanOutcome
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.LibraryScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScanScreen(
    onDone: () -> Unit,
    viewModel: LibraryScanViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val progress by viewModel.scanProgress.collectAsState()
    val running = ui.outcome == LibraryScanOutcome.RUNNING

    LaunchedEffect(Unit) {
        viewModel.startScanIfNeeded()
    }

    BackHandler(enabled = running) {
        viewModel.abort()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            Text(
                when (ui.outcome) {
                    LibraryScanOutcome.RUNNING -> progress?.statusMessage ?: "Starting scan…"
                    LibraryScanOutcome.SUCCESS -> "Scan complete"
                    LibraryScanOutcome.CANCELLED -> "Scan cancelled"
                    LibraryScanOutcome.FAILED -> "Scan failed"
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            if (ui.esdeLinked) {
                Text(
                    "Including ES-DE gamelists and downloaded_media in this scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (running) {
                LinearProgressIndicator(
                    progress = { progress?.progressFraction ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                progress?.let { p ->
                    Text(
                        when (p.stage) {
                            ScanStage.PREPARING -> "Preparing…"
                            ScanStage.SCANNING_SYSTEM -> buildString {
                                append("System ${p.systemsDone + 1} of ${p.systemsTotal}")
                                if (p.systemName.isNotBlank()) append(": ${p.systemName}")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ScanStatGrid(
                        systemsDone = p.systemsDone,
                        systemsTotal = p.systemsTotal,
                        gamesTotal = p.gamesTotal,
                        mediaTotal = p.mediaTotal,
                        unknownFiles = p.unknownFiles,
                        gamesInSystem = p.gamesProcessedInSystem,
                        gamesInSystemTotal = p.gamesInCurrentSystem,
                    )
                } ?: Text(
                    "Loading catalog and media indexes…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                ui.result?.let { result ->
                    ScanStatGrid(
                        systemsDone = result.systemsScanned,
                        systemsTotal = result.systemsScanned,
                        gamesTotal = result.gamesFound,
                        mediaTotal = result.mediaLinked,
                        unknownFiles = result.unknownFiles.size,
                        gamesInSystem = 0,
                        gamesInSystemTotal = 0,
                    )
                }
                ui.message?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (running) {
                OutlinedButton(
                    onClick = viewModel::abort,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abort scan")
                }
            } else {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Done")
                }
            }
    }
}

@Composable
private fun ScanStatGrid(
    systemsDone: Int,
    systemsTotal: Int,
    gamesTotal: Int,
    mediaTotal: Int,
    unknownFiles: Int,
    gamesInSystem: Int,
    gamesInSystemTotal: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScanStatLine("Systems", "$systemsDone / $systemsTotal")
        ScanStatLine("Games found", gamesTotal.toString())
        ScanStatLine("Media linked", mediaTotal.toString())
        if (unknownFiles > 0) {
            ScanStatLine("Unrecognized files", unknownFiles.toString())
        }
        if (gamesInSystemTotal > 0) {
            ScanStatLine(
                "Current system",
                "$gamesInSystem / $gamesInSystemTotal games",
            )
        }
    }
}

@Composable
private fun ScanStatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
