package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeViewModel

@Composable
fun ScrapeScreen(
    onStartWizard: () -> Unit,
    onOpenCredentials: () -> Unit,
    viewModel: ScrapeViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshStats() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Scrape", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Download artwork and metadata into your ORGL data folder. ES-DE is never modified.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        Text("Library", style = MaterialTheme.typography.titleMedium)
        Text(
            "${stats.totalGames} games",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "${stats.scrapedGames} scraped by ORGL",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Last scrape: ${stats.lastScrapeLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (stats.totalGames > 0) {
            val pct = (stats.scrapedGames * 100) / stats.totalGames
            Text(
                "$pct% coverage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onStartWizard,
            modifier = Modifier.fillMaxWidth(),
            enabled = stats.totalGames > 0,
        ) {
            Text("Start scraping wizard")
        }
        OutlinedButton(
            onClick = onOpenCredentials,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (stats.ssConfigured) "Scraping credentials"
                else "Set scraping credentials",
            )
        }
        if (!stats.ssConfigured) {
            Text(
                "ScreenScraper credentials are recommended. Without them, ORGL falls back to libretro-thumbnails only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (stats.totalGames == 0) {
            Text(
                "Scan your library first (Settings → ES-DE / Library).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
