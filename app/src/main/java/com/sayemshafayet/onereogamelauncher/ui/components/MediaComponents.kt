package com.sayemshafayet.onereogamelauncher.ui.components

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sayemshafayet.onereogamelauncher.domain.MediaType
import java.io.File

@Composable
fun GameCoverImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val model = remember(path) {
        when {
            path.isNullOrBlank() -> null
            path.startsWith("content:", ignoreCase = true) ||
                path.startsWith("file:", ignoreCase = true) ||
                path.startsWith("http", ignoreCase = true) -> {
                ImageRequest.Builder(context)
                    .data(Uri.parse(path))
                    .crossfade(true)
                    .build()
            }
            else -> {
                val file = File(path)
                val data: Any = if (file.canRead()) file else path
                ImageRequest.Builder(context)
                    .data(data)
                    .crossfade(true)
                    .build()
            }
        }
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.VideogameAsset,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

fun pickHeroMedia(paths: Map<MediaType, String>): String? =
    paths[MediaType.FANART]
        ?: paths[MediaType.BOX_2D]
        ?: paths[MediaType.BOX_3D]
        ?: paths[MediaType.SCREENSHOT]
        ?: paths[MediaType.MARQUEE]
        ?: paths.values.firstOrNull()

fun pickBoxArt(paths: Map<MediaType, String>): String? =
    paths[MediaType.BOX_2D]
        ?: paths[MediaType.BOX_3D]
        ?: paths[MediaType.SCREENSHOT]
        ?: paths[MediaType.MARQUEE]
        ?: paths.values.firstOrNull()

fun mediaTypeLabel(type: MediaType): String = when (type) {
    MediaType.BOX_2D -> "Box art"
    MediaType.BOX_3D -> "3D box"
    MediaType.SCREENSHOT -> "Screenshot"
    MediaType.TITLE -> "Title screen"
    MediaType.MARQUEE -> "Marquee"
    MediaType.VIDEO -> "Video"
    MediaType.FANART -> "Fan art"
    MediaType.UNKNOWN -> "Media"
}

@Composable
fun GameVideoPlayer(
    path: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uri = remember(path) {
        when {
            path.startsWith("content:", ignoreCase = true) ||
                path.startsWith("file:", ignoreCase = true) -> Uri.parse(path)
            else -> {
                val file = File(path)
                if (file.canRead()) Uri.fromFile(file) else Uri.parse(path)
            }
        }
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                    setVideoURI(uri)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.setVideoURI(uri) },
        )
    }
}

@Composable
fun PulseModifier(enabled: Boolean): Modifier {
    if (!enabled) return Modifier
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    return Modifier.scale(scale)
}
