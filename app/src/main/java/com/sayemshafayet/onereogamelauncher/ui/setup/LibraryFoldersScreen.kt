package com.sayemshafayet.onereogamelauncher.ui.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFoldersScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val romsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onRomsFolderPicked(it) }
    }
    val orglDataPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onOrglDataFolderPicked(it) }
    }

    ui.orglIncompatibleAlert?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissOrglIncompatibleAlert,
            title = { Text("Incompatible ORGL folder") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissOrglIncompatibleAlert) {
                    Text("Choose another folder")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("ROMs folder", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Required. Your game files — one subfolder per system (nes, snes, psx, …). Read-only; ORGL never writes here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(ui.romsDisplay, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { romsPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.romsUri.isBlank()) "Browse for ROMs folder" else "Change ROMs folder")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("ORGL data folder", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Required. Where ORGL stores scraped artwork and other app-owned files (downloaded_media/). " +
                    "Reusing a previous install’s folder keeps existing media. ORGL writes ${OrglDataDirectory.META_FILE_NAME} (spec v${OrglDataDirectory.SPEC_VERSION}).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(ui.orglDataDisplay, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { orglDataPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (ui.orglDataUri.isBlank()) "Browse for ORGL data folder"
                    else "Change ORGL data folder",
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.rescan() },
                enabled = !ui.scanning && (ui.romsPath.isNotBlank() || ui.romsUri.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.scanning) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Text("Rescan library")
            }
            scanProgress?.let {
                Text(
                    "Scanning ${it.systemName}… ${it.gamesFound} (${it.systemsDone}/${it.systemsTotal})",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            ui.scanMessage?.let {
                Text(it, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
