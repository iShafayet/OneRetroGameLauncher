package com.sayemshafayet.onereogamelauncher.ui.setup

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.sayemshafayet.onereogamelauncher.ui.util.rememberPhysicalLandscapeAspectRatio

/** Landscape with physical panel aspect strictly greater than 16:9 — compact Library + setup nav. */
fun isLibraryWideLandscape(
    orientation: Int,
    landscapeAspectRatio: Float,
): Boolean {
    if (orientation != Configuration.ORIENTATION_LANDSCAPE) return false
    return landscapeAspectRatio > 16f / 9f
}

@Composable
fun rememberIsLibraryWideLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    val landscapeAspectRatio = rememberPhysicalLandscapeAspectRatio()
    return remember(configuration.orientation, landscapeAspectRatio) {
        isLibraryWideLandscape(
            orientation = configuration.orientation,
            landscapeAspectRatio = landscapeAspectRatio,
        )
    }
}
