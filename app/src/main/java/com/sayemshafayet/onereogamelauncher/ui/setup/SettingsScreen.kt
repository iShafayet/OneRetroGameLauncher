package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onOpenEsde: () -> Unit,
    onOpenScreenScraper: () -> Unit,
    onOpenRetroAchievements: () -> Unit,
    onOpenHltb: () -> Unit,
    onOpenRetroArch: () -> Unit,
    onOpenCredits: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        Text("Integrations", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            title = "ES-DE / Library",
            subtitle = buildString {
                append("ROMs: ${ui.romsDisplay}")
                append(" · ORGL: ${ui.orglDataDisplay}")
                append(" · ES-DE: ${ui.esdeDataDisplay}")
            },
            onClick = onOpenEsde,
        )
        SettingsNavRow(
            title = "ScreenScraper",
            subtitle = if (ui.ssConfigured) "Signed in" else "Not configured",
            onClick = onOpenScreenScraper,
        )
        SettingsNavRow(
            title = "RetroAchievements",
            subtitle = if (ui.raConfigured) "Signed in" else "Not configured",
            onClick = onOpenRetroAchievements,
        )
        SettingsNavRow(
            title = "HowLongToBeat",
            subtitle = if (ui.hltbEnabled) "Enabled" else "Disabled",
            onClick = onOpenHltb,
        )
        SettingsNavRow(
            title = "RetroArch",
            subtitle = ui.retroArchPkg.ifBlank { "Not set" },
            onClick = onOpenRetroArch,
        )

        Spacer(Modifier.height(16.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = ui.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(mode.name) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.rescan() },
            enabled = !ui.scanning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.scanning) CircularProgressIndicator(modifier = Modifier.height(20.dp))
            else Text("Rescan library")
        }
        scanProgress?.let {
            Text(
                "Scanning ${it.systemName}… ${it.gamesFound} games (${it.systemsDone}/${it.systemsTotal})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        ui.scanMessage?.let {
            Text(it, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(24.dp))
        SettingsNavRow(
            title = "Credits",
            subtitle = "License, data sources, libraries",
            onClick = onOpenCredits,
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 2) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
    HorizontalDivider()
}
