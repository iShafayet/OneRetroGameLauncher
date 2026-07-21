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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.components.CoreDropdown
import com.sayemshafayet.onereogamelauncher.ui.components.EmulatorDropdown
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SystemEmulatorViewModel

@Composable
fun SystemEmulatorSettingsScreen(
    onBack: () -> Unit,
    viewModel: SystemEmulatorViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val isRetroArch = ui.emulatorKey.equals("RETROARCH", ignoreCase = true)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            Text(
                ui.system?.displayName ?: "System",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Default emulator for games in this system. Individual games can override this.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            EmulatorDropdown(
                choices = ui.emulatorChoices,
                selectedKey = ui.emulatorKey,
                onSelected = viewModel::setEmulatorKey,
            )

            if (isRetroArch) {
                if (ui.coreChoices.isNotEmpty()) {
                    CoreDropdown(
                        choices = ui.coreChoices,
                        selectedCore = ui.core,
                        onSelected = viewModel::setCore,
                    )
                }
                OutlinedTextField(
                    value = ui.core,
                    onValueChange = viewModel::setCore,
                    label = { Text("Core filename") },
                    supportingText = { Text("e.g. snes9x_libretro_android.so") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (ui.saved) {
                Text("Saved", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = ui.emulatorKey.isNotBlank(),
            ) {
                Text("Save")
            }
    }
}
