package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.BuildConfig
import com.sayemshafayet.onereogamelauncher.flavor.StoreFlavorLabel

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("One Retro Game Launcher", style = MaterialTheme.typography.headlineSmall)
        Text(
            StoreFlavorLabel.CREDITS,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "GPL-3.0. One-game-at-a-time frontend for retro libraries — ES-DE compatible, external emulators only.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Data sources", style = MaterialTheme.typography.titleMedium)
        Text("• ES-DE system definitions (es_systems / es_find_rules)")
        Text("• ScreenScraper (optional account)")
        Text("• libretro-thumbnails (fallback artwork)")
        Text("• RetroAchievements (optional account)")
        Text("• HowLongToBeat (optional)")

        Text("Libraries", style = MaterialTheme.typography.titleMedium)
        Text(
            "Jetpack Compose, Material 3, Room, DataStore, Hilt, OkHttp, Coil, Kotlin Coroutines",
        )

        Text("License", style = MaterialTheme.typography.titleMedium)
        Text("See LICENSE in the project root (GNU GPL v3).")

        Text(
            "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
