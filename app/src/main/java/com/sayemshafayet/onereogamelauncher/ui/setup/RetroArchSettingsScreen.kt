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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.launch.RetroArchLauncher
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

private const val CUSTOM_OPTION = "Custom…"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroArchSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val known = RetroArchLauncher.KNOWN_PACKAGES
    val isKnown = ui.retroArchPkg in known
    val isCustomSelection = ui.retroArchPkg.isNotBlank() && !isKnown
    var expanded by remember { mutableStateOf(false) }
    var showCustomField by remember(ui.retroArchPkg) {
        mutableStateOf(isCustomSelection)
    }

    val selectedLabel = when {
        showCustomField -> CUSTOM_OPTION
        isKnown -> ui.retroArchPkg
        else -> "Select package"
    }

    Scaffold(
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Preferred RetroArch package used for launches. Pick a known build, or Custom to type any package name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Package") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    known.forEach { pkg ->
                        DropdownMenuItem(
                            text = { Text(pkg) },
                            onClick = {
                                showCustomField = false
                                expanded = false
                                viewModel.setRetroArchPackage(pkg)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(CUSTOM_OPTION) },
                        onClick = {
                            showCustomField = true
                            expanded = false
                            if (isKnown) viewModel.setRetroArchPackage("")
                        },
                    )
                }
            }

            if (showCustomField) {
                OutlinedTextField(
                    value = ui.retroArchPkg,
                    onValueChange = viewModel::setRetroArchPackage,
                    label = { Text("Custom package name") },
                    placeholder = { Text("com.example.retroarch") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Known packages: ${known.joinToString(" · ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
