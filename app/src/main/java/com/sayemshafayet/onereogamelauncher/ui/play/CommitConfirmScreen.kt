package com.sayemshafayet.onereogamelauncher.ui.play

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.PulseModifier
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.CommitConfirmViewModel

@Composable
fun CommitConfirmScreen(
    onConfirmed: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CommitConfirmViewModel = hiltViewModel(),
) {
    val game by viewModel.game.collectAsState()
    val media by viewModel.media.collectAsState()
    val error by viewModel.error.collectAsState()

    val cover = media.firstOrNull { it.type == MediaType.BOX_2D || it.type == MediaType.BOX_3D }?.path
        ?: media.firstOrNull()?.path

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Commit?", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "This will be your only game until you finish or drop it.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        GameCoverImage(
            path = cover,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(0.75f),
        )
        Spacer(Modifier.height(16.dp))
        Text(game?.title ?: "", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
            onClick = { viewModel.commit(onConfirmed) },
            modifier = Modifier
                .fillMaxWidth()
                .then(PulseModifier(true)),
        ) {
            Text("I'm committing")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Not yet")
        }
    }
}
