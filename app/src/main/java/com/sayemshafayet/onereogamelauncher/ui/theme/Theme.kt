package com.sayemshafayet.onereogamelauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayemshafayet.onereogamelauncher.domain.ThemeMode

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

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

/** Fully specified neon scheme — luminous panels, not void-black. */
val NeonColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF041018),
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = NeonCyanOnContainer,
    secondary = NeonMagenta,
    onSecondary = NeonText,
    secondaryContainer = NeonMagentaContainer,
    onSecondaryContainer = NeonMagentaOnContainer,
    tertiary = NeonOrange,
    onTertiary = Color(0xFF1A0A00),
    tertiaryContainer = NeonOrangeContainer,
    onTertiaryContainer = NeonOrangeOnContainer,
    error = NeonError,
    onError = NeonOnError,
    errorContainer = NeonErrorContainer,
    onErrorContainer = NeonText,
    // Semi-transparent so [NeonAtmosphere] plasma reads through scaffolds.
    background = Color(0x991A0A38),
    onBackground = NeonText,
    surface = Color(0xCC2E1868),
    onSurface = NeonText,
    surfaceVariant = Color(0xCC3D2480),
    onSurfaceVariant = NeonTextMuted,
    outline = NeonOutline,
    outlineVariant = NeonOutlineVariant,
    scrim = NeonScrim,
    inverseSurface = NeonInverse,
    inverseOnSurface = Color(0xFF14082E),
    inversePrimary = NeonInversePrimary,
    surfaceTint = NeonCyan,
    surfaceDim = Color(0xBB22124A),
    surfaceBright = Color(0xDD5A2A88),
    surfaceContainerLowest = Color(0xAA180A32),
    surfaceContainerLow = Color(0xBB3A1A6E),
    surfaceContainer = Color(0xCC4A2288),
    surfaceContainerHigh = Color(0xDD0E5A6C),
    surfaceContainerHighest = Color(0xEE6A2A98),
)

private val NeonShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

private val NeonTypography = OrglTypography.copy(
    displayLarge = OrglTypography.displayLarge.copy(
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
    ),
    displayMedium = OrglTypography.displayMedium.copy(letterSpacing = 1.2.sp),
    headlineLarge = OrglTypography.headlineLarge.copy(letterSpacing = 1.sp),
    headlineMedium = OrglTypography.headlineMedium.copy(letterSpacing = 0.8.sp),
    titleLarge = OrglTypography.titleLarge.copy(letterSpacing = 0.6.sp),
    titleMedium = OrglTypography.titleMedium.copy(
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelLarge = TextStyle(
        fontFamily = BrandFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BrandFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BrandFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun OrglTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isPlayMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.NEON -> true
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val neon = themeMode.isNeon()
    val colorScheme = when {
        neon -> NeonColors
        isPlayMode && useDark -> PlayDarkColors
        useDark -> DarkColors
        else -> LightColors
    }
    val palette = if (neon) OrglPalette.Neon else OrglPalette.Brand
    val typography = if (neon) NeonTypography else OrglTypography
    val shapes = if (neon) NeonShapes else Shapes()

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalOrglPalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
        ) {
            if (neon) {
                NeonAtmosphere(content = content)
            } else {
                content()
            }
        }
    }
}
