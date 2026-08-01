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

/** Fully specified calm scheme — soft sage/sand/mist, not stock Material. */
val CalmColors = lightColorScheme(
    primary = CalmSage,
    onPrimary = Color(0xFFF7FBF8),
    primaryContainer = CalmSageContainer,
    onPrimaryContainer = CalmOnSageContainer,
    secondary = CalmSandDeep,
    onSecondary = Color(0xFFFFF8F0),
    secondaryContainer = CalmSandContainer,
    onSecondaryContainer = CalmOnSandContainer,
    tertiary = CalmLavender,
    onTertiary = Color(0xFFF8F8FC),
    tertiaryContainer = CalmLavenderContainer,
    onTertiaryContainer = CalmOnLavenderContainer,
    error = CalmError,
    onError = CalmOnError,
    errorContainer = CalmErrorContainer,
    onErrorContainer = Color(0xFF3A1818),
    background = Color(0x99E8F0EA),
    onBackground = CalmInk,
    surface = Color(0xCCF4F7F4),
    onSurface = CalmInk,
    surfaceVariant = Color(0xCCE4EBE6),
    onSurfaceVariant = CalmInkMuted,
    outline = CalmOutline,
    outlineVariant = CalmOutlineVariant,
    scrim = CalmScrim,
    inverseSurface = CalmInverse,
    inverseOnSurface = CalmFog,
    inversePrimary = CalmInversePrimary,
    surfaceTint = CalmSage,
    surfaceDim = Color(0xBBD8E4DC),
    surfaceBright = Color(0xEEFAFCF9),
    surfaceContainerLowest = Color(0xAAF7FAF7),
    surfaceContainerLow = Color(0xBBE8F0EA),
    surfaceContainer = Color(0xCCDCE8E0),
    surfaceContainerHigh = Color(0xDDD4E0EC),
    surfaceContainerHighest = Color(0xEEE8DFD4),
)

/** Fully specified GC scheme — solid GameCube controller plastics (no washes). */
val GcColors = darkColorScheme(
    primary = GcBody,
    onPrimary = GcSilver,
    primaryContainer = GcBodyDark,
    onPrimaryContainer = GcSilver,
    secondary = GcGreen,
    onSecondary = GcSilver,
    secondaryContainer = GcGreen,
    onSecondaryContainer = GcSilver,
    tertiary = GcYellow,
    onTertiary = GcBodyDark,
    tertiaryContainer = GcYellow,
    onTertiaryContainer = GcBodyDark,
    error = GcRed,
    onError = GcSilver,
    errorContainer = GcRed,
    onErrorContainer = GcSilver,
    background = GcBodyDark,
    onBackground = GcSilver,
    surface = GcBody,
    onSurface = GcSilver,
    surfaceVariant = GcBodyLight,
    onSurfaceVariant = GcSilver,
    outline = GcSilver,
    outlineVariant = GcBodyDark,
    scrim = GcBodyDark,
    inverseSurface = GcSilver,
    inverseOnSurface = GcBodyDark,
    inversePrimary = GcBody,
    surfaceTint = GcBody,
    surfaceDim = GcBodyDark,
    surfaceBright = GcBodyLight,
    surfaceContainerLowest = GcBodyDark,
    surfaceContainerLow = GcBody,
    surfaceContainer = GcBody,
    surfaceContainerHigh = GcBodyLight,
    surfaceContainerHighest = GcBodyLight,
)

/** Fully specified SNES scheme — light grey shell, purple accents (NA Super NES). */
val SnesColors = lightColorScheme(
    // Darker purple — primary buttons / controls
    primary = SnesDark,
    onPrimary = SnesLighter,
    // Lilac — soft containers / chrome companion
    primaryContainer = SnesLight,
    onPrimaryContainer = SnesDark,
    secondary = SnesDark,
    onSecondary = SnesLighter,
    // Tonal buttons / selected chips (reads on lilac navbar)
    secondaryContainer = SnesDark,
    onSecondaryContainer = SnesLighter,
    tertiary = SnesMedium,
    onTertiary = SnesLighter,
    tertiaryContainer = SnesMedium,
    onTertiaryContainer = SnesLighter,
    error = SnesBorder,
    onError = SnesLighter,
    errorContainer = SnesMedium,
    onErrorContainer = SnesLighter,
    background = SnesBackground,
    onBackground = SnesText,
    surface = SnesPanel,
    onSurface = SnesText,
    surfaceVariant = SnesLighter,
    onSurfaceVariant = SnesText,
    outline = SnesBorder,
    outlineVariant = SnesMedium,
    scrim = SnesBorder.copy(alpha = 0.5f),
    inverseSurface = SnesBorder,
    inverseOnSurface = SnesLighter,
    inversePrimary = SnesLight,
    surfaceTint = SnesDark,
    surfaceDim = SnesBackground,
    surfaceBright = SnesLighter,
    surfaceContainerLowest = SnesBackground,
    surfaceContainerLow = SnesPanel,
    surfaceContainer = SnesPanel,
    surfaceContainerHigh = SnesLighter,
    surfaceContainerHighest = SnesLighter,
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

private val CalmShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val CalmTypography = OrglTypography.copy(
    displayLarge = OrglTypography.displayLarge.copy(
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Medium,
    ),
    displayMedium = OrglTypography.displayMedium.copy(letterSpacing = 0.sp),
    headlineLarge = OrglTypography.headlineLarge.copy(
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineMedium = OrglTypography.headlineMedium.copy(letterSpacing = 0.sp),
    titleLarge = OrglTypography.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = OrglTypography.titleMedium.copy(letterSpacing = 0.1.sp),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.15.sp,
    ),
)

private val GcShapes = Shapes(
    // Soft cube — GameCube’s friendly rounded chassis.
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val GcTypography = OrglTypography.copy(
    displayLarge = OrglTypography.displayLarge.copy(
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Bold,
    ),
    displayMedium = OrglTypography.displayMedium.copy(letterSpacing = 0.sp),
    headlineLarge = OrglTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = OrglTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

/** Soft “16-bit console” rounding — friendly like the SNES shell. */
private val SnesShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val SnesTypography = OrglTypography.copy(
    displayLarge = OrglTypography.displayLarge.copy(
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold,
    ),
    displayMedium = OrglTypography.displayMedium.copy(letterSpacing = 0.8.sp),
    headlineLarge = OrglTypography.headlineLarge.copy(
        letterSpacing = 0.6.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = OrglTypography.headlineMedium.copy(letterSpacing = 0.4.sp),
    titleLarge = OrglTypography.titleLarge.copy(
        letterSpacing = 0.5.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = OrglTypography.titleMedium.copy(
        letterSpacing = 0.6.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.8.sp,
    ),
)

@Composable
fun OrglTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isPlayMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.NEON, ThemeMode.GC, ThemeMode.DARK -> true
        ThemeMode.CALM, ThemeMode.SNES, ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val neon = themeMode.isNeon()
    val calm = themeMode.isCalm()
    val gc = themeMode.isGc()
    val snes = themeMode.isSnes()
    val colorScheme = when {
        neon -> NeonColors
        calm -> CalmColors
        gc -> GcColors
        snes -> SnesColors
        isPlayMode && useDark -> PlayDarkColors
        useDark -> DarkColors
        else -> LightColors
    }
    val palette = when {
        neon -> OrglPalette.Neon
        calm -> OrglPalette.Calm
        gc -> OrglPalette.Gc
        snes -> OrglPalette.Snes
        else -> OrglPalette.Brand
    }
    val typography = when {
        neon -> NeonTypography
        calm -> CalmTypography
        gc -> GcTypography
        snes -> SnesTypography
        else -> OrglTypography
    }
    val shapes = when {
        neon -> NeonShapes
        calm -> CalmShapes
        gc -> GcShapes
        snes -> SnesShapes
        else -> Shapes()
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalOrglPalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
        ) {
            when {
                neon -> NeonAtmosphere(content = content)
                calm -> CalmAtmosphere(content = content)
                else -> content()
            }
        }
    }
}
