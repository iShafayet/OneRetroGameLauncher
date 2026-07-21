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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsdeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val linked = ui.esdeDataUri.isNotBlank() || ui.esdeDataPath.isNotBlank()
    val esdeDataPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onEsdeDataFolderPicked(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ES-DE") },
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
            Text(
                "Optional. Point ORGL at ES-DE’s application data folder to use its downloaded_media/ artwork and gamelists/ metadata as a read-only fallback. ORGL never writes to ES-DE.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                if (linked) "Linked" else "Not linked",
                style = MaterialTheme.typography.titleMedium,
                color = if (linked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(ui.esdeDataDisplay, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { esdeDataPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (linked) "Change ES-DE data folder" else "Link ES-DE data folder")
            }

            if (linked) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.rescan() },
                    enabled = !ui.scanning && (ui.romsPath.isNotBlank() || ui.romsUri.isNotBlank()),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (ui.scanning) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("Rescan to refresh ES-DE media")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.unlinkEsde() },
                    enabled = !ui.scanning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlink ES-DE")
                }
                Text(
                    "Unlinking clears the ES-DE folder setting and removes ES-DE-linked media from ORGL’s cache. Your ES-DE install is not modified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
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
