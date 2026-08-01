package com.sayemshafayet.onereogamelauncher.ui.legal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sayemshafayet.onereogamelauncher.ui.theme.CalmColors
import com.sayemshafayet.onereogamelauncher.ui.theme.GcColors
import com.sayemshafayet.onereogamelauncher.ui.theme.NeonColors
import com.sayemshafayet.onereogamelauncher.ui.theme.isOrglCalmTheme
import com.sayemshafayet.onereogamelauncher.ui.theme.isOrglGcTheme
import com.sayemshafayet.onereogamelauncher.ui.theme.isOrglNeonTheme

/**
 * Legal reading surface — follows app theme. Stylized modes use their full schemes;
 * otherwise a neutral static light/dark (no wallpaper colors).
 */
@Composable
fun LegalDocumentTheme(content: @Composable () -> Unit) {
    val colorScheme = when {
        isOrglNeonTheme() -> NeonColors
        isOrglCalmTheme() -> CalmColors
        isOrglGcTheme() -> GcColors
        isSystemInDarkTheme() -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

@Composable
fun LegalDocumentSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LegalDocumentTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Box(modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
