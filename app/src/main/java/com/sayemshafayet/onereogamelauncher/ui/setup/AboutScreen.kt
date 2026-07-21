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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenCredits: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            Text("One Retro Game Launcher", style = MaterialTheme.typography.headlineMedium)
            Text(
                "ORGL is a deliberate front-end for people who love retro games — and want to finish them.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Mission", style = MaterialTheme.typography.titleLarge)
            Text(
                "Help you play one game at a time. Pick something, commit to it, and give it a fair run — " +
                    "instead of drowning in an endless “maybe later” shelf.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text("Vision", style = MaterialTheme.typography.titleLarge)
            Text(
                "A calm library that stays out of the way: your ROMs stay yours, artwork lives in your ORGL data folder, " +
                    "and emulators stay external. Optional integrations (ES-DE media, ScreenScraper, RetroAchievements, " +
                    "HowLongToBeat) enrich the experience without locking you in.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text("How we work", style = MaterialTheme.typography.titleLarge)
            Text(
                "• Setup mode — organize systems, scrape media, tune emulators.\n" +
                    "• Play mode — commit to a single game until you finish or drop it.\n" +
                    "• Your folders — ROMs are read-only; scrapes write only to ORGL data.\n" +
                    "• Your choice of emulator — RetroArch or standalone, per system or per game.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenCredits,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Credits")
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back to library")
            }
    }
}
