package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import com.sayemshafayet.onereogamelauncher.ui.components.GameCoverImage
import com.sayemshafayet.onereogamelauncher.ui.components.GameVideoPlayer
import com.sayemshafayet.onereogamelauncher.ui.components.mediaTypeLabel
import com.sayemshafayet.onereogamelauncher.ui.viewmodel.MediaViewerViewModel

@Composable
fun MediaViewerScreen(
    viewModel: MediaViewerViewModel = hiltViewModel(),
) {
    val media by viewModel.media.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        val item = media
        if (item == null) {
            Text(
                "Media not found",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            when (item.type) {
                MediaType.VIDEO -> {
                    GameVideoPlayer(
                        path = item.path,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    GameCoverImage(
                        path = item.path,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Text(
                mediaTypeLabel(item.type),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )
        }
    }
}
