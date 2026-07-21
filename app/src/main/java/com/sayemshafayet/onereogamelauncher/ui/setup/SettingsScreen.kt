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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
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
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onOpenLibraryFolders: () -> Unit,
    onOpenEsde: () -> Unit,
    onOpenScreenScraper: () -> Unit,
    onOpenRetroAchievements: () -> Unit,
    onOpenHltb: () -> Unit,
    onOpenRetroArch: () -> Unit,
    onStartScan: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val esdeLinked = ui.esdeDataUri.isNotBlank() || ui.esdeDataPath.isNotBlank()
    val romsConfigured = ui.romsPath.isNotBlank() || ui.romsUri.isNotBlank()
    val firstFocus = rememberOrlgFocusRequester()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            title = "Folders",
            subtitle = buildString {
                append("ROMs: ${ui.romsDisplay}")
                append(" · ORGL: ${ui.orglDataDisplay}")
            },
            modifier = Modifier.orlgListFocus(0, firstFocus).orlgFocusable(onOpenLibraryFolders),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onStartScan,
            enabled = romsConfigured,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Rescan library")
        }
        Text(
            "Scans ROMs, ORGL media, and ES-DE metadata/media in one pass.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(20.dp))
        Text("Integrations", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            title = "ES-DE",
            subtitle = if (esdeLinked) "Linked · ${ui.esdeDataDisplay}" else "Optional media fallback",
            modifier = Modifier.orlgFocusable(onOpenEsde),
        )
        SettingsNavRow(
            title = "ScreenScraper",
            subtitle = "Under construction — use ES-DE for media",
            modifier = Modifier.orlgFocusable(onOpenScreenScraper),
        )
        SettingsNavRow(
            title = "RetroAchievements",
            subtitle = if (ui.raConfigured) "Signed in" else "Not configured",
            modifier = Modifier.orlgFocusable(onOpenRetroAchievements),
        )
        SettingsNavRow(
            title = "HowLongToBeat",
            subtitle = if (ui.hltbEnabled) "Enabled" else "Disabled",
            modifier = Modifier.orlgFocusable(onOpenHltb),
        )
        SettingsNavRow(
            title = "RetroArch",
            subtitle = ui.retroArchPkg.ifBlank { "Not set" },
            modifier = Modifier.orlgFocusable(onOpenRetroArch),
        )

        Spacer(Modifier.height(20.dp))
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
        Spacer(Modifier.height(8.dp))
    }
    OrlgInitialFocus(firstFocus)
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 2) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        },
        modifier = modifier.fillMaxWidth(),
    )
    HorizontalDivider()
}
