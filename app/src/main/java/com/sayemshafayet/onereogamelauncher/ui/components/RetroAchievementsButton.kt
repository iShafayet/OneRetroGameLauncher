package com.sayemshafayet.onereogamelauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.domain.RaButtonState
import com.sayemshafayet.onereogamelauncher.domain.RaVisualState

@Composable
fun RetroAchievementsButton(
    state: RaButtonState,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = when (state.visual) {
        RaVisualState.NO_GAME -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        RaVisualState.SIGN_IN_REQUIRED -> MaterialTheme.colorScheme.onSurfaceVariant
        RaVisualState.UNSUPPORTED_SETUP -> MaterialTheme.colorScheme.onSurfaceVariant
        RaVisualState.ERROR -> MaterialTheme.colorScheme.error
        RaVisualState.READY -> MaterialTheme.colorScheme.primary
    }

    val errorOverlay = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClick, enabled = state.enabled && !loading) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "RetroAchievements",
                    tint = tint,
                    modifier = Modifier.size(28.dp),
                )
                if (state.visual == RaVisualState.UNSUPPORTED_SETUP) {
                    Canvas(Modifier.size(28.dp)) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.85f),
                            start = Offset(size.width * 0.15f, size.height * 0.85f),
                            end = Offset(size.width * 0.85f, size.height * 0.15f),
                            strokeWidth = 2.5f,
                        )
                    }
                }
                if (state.visual == RaVisualState.ERROR) {
                    Canvas(Modifier.size(28.dp)) {
                        drawLine(
                            color = errorOverlay,
                            start = Offset(size.width * 0.2f, size.height * 0.2f),
                            end = Offset(size.width * 0.8f, size.height * 0.8f),
                            strokeWidth = 2.5f,
                        )
                        drawLine(
                            color = errorOverlay,
                            start = Offset(size.width * 0.8f, size.height * 0.2f),
                            end = Offset(size.width * 0.2f, size.height * 0.8f),
                            strokeWidth = 2.5f,
                        )
                    }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                "RetroAchievements",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
            if (state.subtitle.isNotBlank()) {
                Text(
                    if (loading) "Checking…" else state.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
