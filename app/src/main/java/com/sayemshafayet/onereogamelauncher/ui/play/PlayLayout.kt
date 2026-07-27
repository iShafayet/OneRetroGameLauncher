package com.sayemshafayet.onereogamelauncher.ui.play

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Landscape with aspect ratio at least 16:9 — phone landscape, tablet, TV.
 * Two-pane layouts must be height-safe (size art by height, scroll panes).
 */
fun isWidePlayLayout(
    orientation: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
): Boolean {
    if (orientation != Configuration.ORIENTATION_LANDSCAPE) return false
    val ratio = screenWidthDp.toFloat() / screenHeightDp.coerceAtLeast(1).toFloat()
    return ratio >= 16f / 9f
}

/** Short widescreen (typical phone landscape) — denser two-pane spacing. */
fun isCompactWidePlayLayout(screenHeightDp: Int): Boolean = screenHeightDp < 500

@Composable
fun rememberIsWidePlayLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    ) {
        isWidePlayLayout(
            orientation = configuration.orientation,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
        )
    }
}

@Composable
fun rememberIsCompactWidePlayLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenHeightDp) {
        isCompactWidePlayLayout(configuration.screenHeightDp)
    }
}

private val PlayContentMaxWidth = 960.dp

/** Centers Play content and caps width on very large displays. */
@Composable
fun PlayWideContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .widthIn(max = PlayContentMaxWidth)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}
