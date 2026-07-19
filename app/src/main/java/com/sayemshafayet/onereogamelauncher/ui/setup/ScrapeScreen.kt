package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.ScrapeViewModel

@Composable
fun ScrapeScreen(
    viewModel: ScrapeViewModel = hiltViewModel(),
) {
    val systems by viewModel.systems.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Scrape artwork", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Downloads box art and media into your ORGL data folder (downloaded_media/). Configure that under Settings → ES-DE / Library. ES-DE is never written. Progress appears in the notification shade.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.scrapeAll() }, modifier = Modifier.fillMaxWidth()) {
            Text("Scrape all systems")
        }
        message?.let {
            Text(it, modifier = Modifier.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(systems, key = { it.id }) { sys ->
                ListItem(
                    headlineContent = { Text(sys.displayName) },
                    supportingContent = { Text(sys.folderName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.scrapeSystem(sys.id, sys.folderName)
                        },
                )
                HorizontalDivider()
            }
        }
    }
}
