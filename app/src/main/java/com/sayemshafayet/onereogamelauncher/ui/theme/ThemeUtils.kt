package com.sayemshafayet.onereogamelauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode

/** True when [ThemeMode.NEON] is the active app appearance. */
fun ThemeMode.isNeon(): Boolean = this == ThemeMode.NEON

/**
 * Reads [LocalThemeMode] — use in Compose UI instead of
 * `LocalThemeMode.current == ThemeMode.NEON`.
 */
@Composable
@ReadOnlyComposable
fun isOrglNeonTheme(): Boolean = LocalThemeMode.current.isNeon()
