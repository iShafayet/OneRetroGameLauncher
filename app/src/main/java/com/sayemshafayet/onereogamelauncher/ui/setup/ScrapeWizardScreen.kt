package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.ScrapeGameFilter
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeWizardStep
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapeWizardScreen(
    onBack: () -> Unit,
    viewModel: ScrapeViewModel = hiltViewModel(),
) {
    val wizard by viewModel.wizard.collectAsState()
    val session by viewModel.session.collectAsState()

    LaunchedEffect(Unit) {
        if (wizard.systems.isEmpty()) viewModel.prepareWizard()
    }

    Scaffold(
    ) { padding ->
        when (wizard.step) {
            ScrapeWizardStep.SYSTEMS -> SystemsStep(
                modifier = Modifier.padding(padding),
                wizard = wizard,
                onToggle = viewModel::toggleSystem,
                onSelectAll = { viewModel.selectAllSystems(true) },
                onSelectNone = { viewModel.selectAllSystems(false) },
                onNext = viewModel::goToOptions,
            )
            ScrapeWizardStep.OPTIONS -> OptionsStep(
                modifier = Modifier.padding(padding),
                wizard = wizard,
                onFilter = viewModel::setFilter,
                onRetry = viewModel::setRetryThreshold,
                onDelay = viewModel::setRetryDelaySec,
                onBack = viewModel::goToSystems,
                onStart = viewModel::startScrape,
            )
            ScrapeWizardStep.PROGRESS -> ProgressStep(
                modifier = Modifier.padding(padding),
                session = session,
                onCancel = viewModel::cancelScrape,
                onDone = {
                    viewModel.refreshStats()
                    onBack()
                },
            )
        }
    }
}

@Composable
private fun SystemsStep(
    modifier: Modifier,
    wizard: com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeWizardUi,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            "Only systems with games are listed. Choose which to include.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onSelectAll) { Text("Select all") }
            TextButton(onClick = onSelectNone) { Text("Select none") }
        }
        if (wizard.loading) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(wizard.systems, key = { it.system.id }) { row ->
                    ListItem(
                        headlineContent = { Text(row.system.displayName) },
                        supportingContent = {
                            Text("${row.gameCount} games · ${row.scrapedCount} scraped")
                        },
                        leadingContent = {
                            Checkbox(
                                checked = row.selected,
                                onCheckedChange = { onToggle(row.system.id) },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = row.selected,
                                onClick = { onToggle(row.system.id) },
                                role = Role.Checkbox,
                            ),
                    )
                    HorizontalDivider()
                }
            }
        }
        wizard.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = !wizard.loading && wizard.systems.any { it.selected },
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun OptionsStep(
    modifier: Modifier,
    wizard: com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeWizardUi,
    onFilter: (ScrapeGameFilter) -> Unit,
    onRetry: (Int) -> Unit,
    onDelay: (Int) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Which games?", style = MaterialTheme.typography.titleMedium)
        ScrapeGameFilter.entries.forEach { filter ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = wizard.filter == filter,
                        onClick = { onFilter(filter) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = wizard.filter == filter,
                    onClick = { onFilter(filter) },
                )
                Text(filter.label, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text(
            "About ${wizard.estimatedGames} games will be queued",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        Text("Retry threshold: ${wizard.retryThreshold}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = wizard.retryThreshold.toFloat(),
            onValueChange = { onRetry(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
        )
        Text(
            "Attempts per game before marking as failed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Retry delay: ${wizard.retryDelaySec}s", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = wizard.retryDelaySec.toFloat(),
            onValueChange = { onDelay(it.toInt()) },
            valueRange = 0f..60f,
            steps = 11,
        )
        Text(
            "Wait between retries (helps with rate limits)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        wizard.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f),
                enabled = wizard.estimatedGames > 0,
            ) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun ProgressStep(
    modifier: Modifier,
    session: com.sayemshafayet.onereogamelauncher.domain.ScrapeSessionState,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.running, session.finished) {
        while (session.running && !session.finished) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
        nowMs = System.currentTimeMillis()
    }
    val elapsedMs = when {
        session.startedAtMs == null -> 0L
        session.finishedAtMs != null -> (session.finishedAtMs - session.startedAtMs).coerceAtLeast(0L)
        else -> (nowMs - session.startedAtMs).coerceAtLeast(0L)
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (session.running || (!session.finished && session.total > 0)) {
            Text(session.currentTitle.ifBlank { "Starting…" }, style = MaterialTheme.typography.titleLarge)
            if (session.currentSystem.isNotBlank()) {
                Text(
                    session.currentSystem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = {
                    if (session.total <= 0) 0f
                    else session.index.toFloat() / session.total.toFloat()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${session.index} / ${session.total}")
        } else if (session.finished) {
            Text(
                if (session.cancelled) "Scrape cancelled" else "Scrape finished",
                style = MaterialTheme.typography.headlineSmall,
            )
        } else {
            CircularProgressIndicator()
            Text("Preparing…")
        }

        Spacer(Modifier.height(8.dp))
        StatLine("Completed", session.completed.toString())
        StatLine("Failed", session.failed.toString())
        StatLine("Skipped", session.skipped.toString())
        StatLine("Pending", session.pending.toString())
        StatLine("Elapsed", formatElapsed(elapsedMs))

        session.lastError?.let {
            Text(
                "Last error: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.weight(1f))
        if (session.running) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        } else if (session.finished) {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private val ScrapeGameFilter.label: String
    get() = when (this) {
        ScrapeGameFilter.ALL -> "All games"
        ScrapeGameFilter.MISSING_METADATA -> "Those missing metadata"
        ScrapeGameFilter.MISSING_ANY_MEDIA -> "Those missing any media"
        ScrapeGameFilter.MISSING_VIDEO -> "Those missing videos"
    }

private fun formatElapsed(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
