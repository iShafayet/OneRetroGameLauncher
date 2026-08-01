package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode
import com.sayemshafayet.onereogamelauncher.ui.input.OrglFilterChip
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenLibraryFolders: () -> Unit,
    onOpenEsde: () -> Unit,
    onOpenScreenScraper: () -> Unit,
    onOpenRetroAchievements: () -> Unit,
    onOpenHltb: () -> Unit,
    onOpenRetroArch: () -> Unit,
    onOpenPlaySlots: () -> Unit,
    onOpenDatabase: () -> Unit,
    onOpenSystemInfo: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenCredits: () -> Unit,
    onStartScan: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val esdeLinked = ui.esdeDataUri.isNotBlank() || ui.esdeDataPath.isNotBlank()
    val romsConfigured = ui.romsPath.isNotBlank() || ui.romsUri.isNotBlank()
    val orglConfigured = ui.orglDataUri.isNotBlank() || ui.orglDataPath.isNotBlank()
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
                append(if (romsConfigured) "ROMs directory set" else "ROMs directory not set")
                append(" · ")
                append(if (orglConfigured) "ORGL directory set" else "ORGL directory not set")
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
            subtitle = if (esdeLinked) {
                "Connected"
            } else {
                "Connect ES-DE to use game artwork and information"
            },
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
        Text("Play", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            title = "Multiple Now Playing Slots",
            subtitle = playSlotCountLabel(ui.playSlotCount),
            modifier = Modifier.orlgFocusable(onOpenPlaySlots),
        )

        Spacer(Modifier.height(20.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                OrglFilterChip(
                    selected = ui.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ListItem(
            headlineContent = { Text("Favorites section") },
            supportingContent = { Text("Virtual system at the top of the library") },
            trailingContent = {
                Switch(
                    checked = ui.libraryShowFavorites,
                    onCheckedChange = viewModel::setLibraryShowFavorites,
                )
            },
            modifier = Modifier.orlgFocusable(onClick = { viewModel.setLibraryShowFavorites(!ui.libraryShowFavorites) }),
        )
        ListItem(
            headlineContent = { Text("Wishlist section") },
            supportingContent = { Text("Play-queue games highlighted in Play suggestions") },
            trailingContent = {
                Switch(
                    checked = ui.libraryShowWishlist,
                    onCheckedChange = viewModel::setLibraryShowWishlist,
                )
            },
            modifier = Modifier.orlgFocusable(onClick = { viewModel.setLibraryShowWishlist(!ui.libraryShowWishlist) }),
        )
        ListItem(
            headlineContent = { Text("Recent section") },
            supportingContent = { Text("Recently played games across all systems") },
            trailingContent = {
                Switch(
                    checked = ui.libraryShowRecent,
                    onCheckedChange = viewModel::setLibraryShowRecent,
                )
            },
            modifier = Modifier.orlgFocusable(onClick = { viewModel.setLibraryShowRecent(!ui.libraryShowRecent) }),
        )

        Spacer(Modifier.height(20.dp))
        Text("Debug", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            title = "Database",
            subtitle = if (orglConfigured) {
                "Sync and data maintenance"
            } else {
                "ORGL directory not set"
            },
            modifier = Modifier.orlgFocusable(onOpenDatabase),
        )
        SettingsNavRow(
            title = "System Info",
            subtitle = "Display, device, and layout breakpoints",
            modifier = Modifier.orlgFocusable(onOpenSystemInfo),
        )

        Spacer(Modifier.height(20.dp))
        Text("Information", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        SettingsNavRow(
            title = "About",
            subtitle = "What ORGL is and how it works",
            modifier = Modifier.orlgFocusable(onOpenAbout),
        )
        SettingsNavRow(
            title = "Credits",
            subtitle = "Contributors and acknowledgements",
            modifier = Modifier.orlgFocusable(onOpenCredits),
        )
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
