package com.sayemshafayet.onereogamelauncher.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.BuildConfig
import com.sayemshafayet.onereogamelauncher.flavor.StoreFlavorLabel
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable

private const val LicenseUrl =
    "https://github.com/iShafayet/OneRetroGameLauncher/blob/main/LICENSE"

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val openLicense: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LicenseUrl)))
        }
    }

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
            "Created by Sayem Shafayet. GPL-3.0 — a one-game-at-a-time frontend for retro libraries " +
                "(ES-DE compatible, external emulators only).",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Website: oneretrogamelauncher.com · Source: github.com/iShafayet/OneRetroGameLauncher",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .orlgFocusable(onClick = openLicense)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "GNU GPL v3",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Opens in browser",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
