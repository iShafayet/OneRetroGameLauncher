package com.sayemshafayet.onereogamelauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode

fun ThemeMode.isNeon(): Boolean = this == ThemeMode.NEON

fun ThemeMode.isCalm(): Boolean = this == ThemeMode.CALM

fun ThemeMode.isGc(): Boolean = this == ThemeMode.GC

/** Stylized themes that draw a full-app atmosphere under translucent chrome. */
fun ThemeMode.usesAtmosphere(): Boolean = isNeon() || isCalm()

@Composable
@ReadOnlyComposable
fun isOrglNeonTheme(): Boolean = LocalThemeMode.current.isNeon()

@Composable
@ReadOnlyComposable
fun isOrglCalmTheme(): Boolean = LocalThemeMode.current.isCalm()

@Composable
@ReadOnlyComposable
fun isOrglGcTheme(): Boolean = LocalThemeMode.current.isGc()

@Composable
@ReadOnlyComposable
fun usesOrglAtmosphereTheme(): Boolean = LocalThemeMode.current.usesAtmosphere()

/** Accent for top bar / nav chrome in stylized themes; Unspecified = Material default. */
@Composable
@ReadOnlyComposable
fun orglChromeAccentColor(): Color = when {
    isOrglNeonTheme() -> NeonCyan
    isOrglCalmTheme() -> CalmSage
    isOrglGcTheme() -> GcSilver
    else -> Color.Unspecified
}
