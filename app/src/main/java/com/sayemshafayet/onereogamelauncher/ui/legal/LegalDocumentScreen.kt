package com.sayemshafayet.onereogamelauncher.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.legal.LegalDocumentKind
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun LegalDocumentScreen(
    document: LegalDocumentKind,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val body = remember(document) {
        runCatching {
            context.assets.open(document.assetPath).use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        }.getOrElse { "Could not load ${document.title}." }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            document.title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(document.webUrl)))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open on website")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
        Spacer(Modifier.height(16.dp))
    }
}
