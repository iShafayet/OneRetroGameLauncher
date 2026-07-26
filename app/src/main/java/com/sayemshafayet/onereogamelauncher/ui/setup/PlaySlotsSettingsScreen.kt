package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.prefs.MAX_PLAY_SLOTS
import com.sayemshafayet.onereogamelauncher.data.prefs.MIN_PLAY_SLOTS
import com.sayemshafayet.onereogamelauncher.ui.input.OrglFilterChip
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

@Composable
fun PlaySlotsSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "Multiple Now Playing Slots",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "ORGL is built around focused play — but you can commit to more than one game at a time. " +
                "Each slot is independent: finishing, dropping, or pausing one slot does not affect the others.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "In Play mode, use the slot bar to switch between your active runs. Empty slots can be filled " +
                "from the game picker without disturbing games in other slots.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Number of slots",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (MIN_PLAY_SLOTS..MAX_PLAY_SLOTS).forEach { count ->
                OrglFilterChip(
                    selected = ui.playSlotCount == count,
                    onClick = { viewModel.setPlaySlotCount(count) },
                    label = {
                        Text(if (count == 1) "1 slot" else "$count slots")
                    },
                )
            }
        }
    }
}

fun playSlotCountLabel(count: Int): String =
    if (count == 1) "1 slot" else "$count slots"
