package com.sayemshafayet.onereogamelauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode

private val LightColors = lightColorScheme(
    primary = TealGlow,
    onPrimary = Mist,
    secondary = AmberAccent,
    onSecondary = InkDeep,
    tertiary = AmberSoft,
    background = Mist,
    surface = Mist,
    onBackground = InkDeep,
    onSurface = InkDeep,
)

private val DarkColors = darkColorScheme(
    primary = TealGlow,
    onPrimary = Mist,
    secondary = AmberAccent,
    onSecondary = InkDeep,
    tertiary = AmberSoft,
    background = InkDeep,
    surface = InkMid,
    onBackground = Mist,
    onSurface = Mist,
    surfaceVariant = InkLight,
)

private val PlayDarkColors = darkColorScheme(
    primary = AmberAccent,
    onPrimary = InkDeep,
    secondary = TealGlow,
    onSecondary = Mist,
    tertiary = AmberSoft,
    background = PlaySurface,
    surface = PlaySurfaceVariant,
    onBackground = Mist,
    onSurface = Mist,
    surfaceVariant = InkLight,
)

@Composable
fun OrglTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isPlayMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        isPlayMode && useDark -> PlayDarkColors
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OrglTypography,
        content = content,
    )
}
