package com.sayemshafayet.onereogamelauncher.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.math.min

/**
 * Physical panel size in pixels (display mode), not the app window after system bars.
 * [Configuration.screenWidthDp]/[Configuration.screenHeightDp] shrink with insets and
 * inflate landscape aspect ratios on true 16:9 handhelds.
 */
fun physicalDisplaySizePx(context: Context): Pair<Int, Int>? {
    val mode = context.display?.mode ?: return null
    val width = mode.physicalWidth
    val height = mode.physicalHeight
    if (width <= 0 || height <= 0) return null
    return width to height
}

/** Longer edge / shorter edge of the physical panel (orientation-independent). */
fun physicalLandscapeAspectRatio(context: Context): Float? {
    val (widthPx, heightPx) = physicalDisplaySizePx(context) ?: return null
    val longer = max(widthPx, heightPx)
    val shorter = min(widthPx, heightPx).coerceAtLeast(1)
    return longer.toFloat() / shorter.toFloat()
}

/** Fallback when physical mode is unavailable: use configuration dp (longer / shorter). */
fun configurationLandscapeAspectRatio(
    screenWidthDp: Int,
    screenHeightDp: Int,
): Float {
    val longer = max(screenWidthDp, screenHeightDp)
    val shorter = min(screenWidthDp, screenHeightDp).coerceAtLeast(1)
    return longer.toFloat() / shorter.toFloat()
}

@Composable
fun rememberPhysicalLandscapeAspectRatio(): Float {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val mode = context.display?.mode
    return remember(
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        mode?.physicalWidth,
        mode?.physicalHeight,
    ) {
        physicalLandscapeAspectRatio(context)
            ?: configurationLandscapeAspectRatio(
                screenWidthDp = configuration.screenWidthDp,
                screenHeightDp = configuration.screenHeightDp,
            )
    }
}
