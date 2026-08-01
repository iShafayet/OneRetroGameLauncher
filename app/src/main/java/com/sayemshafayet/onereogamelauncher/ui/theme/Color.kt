package com.sayemshafayet.onereogamelauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val InkDeep = Color(0xFF0B1C24)
val InkMid = Color(0xFF122A35)
val InkLight = Color(0xFF1A3845)
val AmberAccent = Color(0xFFE8A838)
val AmberSoft = Color(0xFFF5C76B)
val TealGlow = Color(0xFF2A6B7A)
val Mist = Color(0xFFE8EDF0)
val PlaySurface = Color(0xFF080F14)
val PlaySurfaceVariant = Color(0xFF101C24)

/** Sampled from ORGL logo (cyan “RETRO” / screen). */
val LogoCyan = Color(0xFF00D8F8)
val LogoPink = Color(0xFFF81880)
val LogoOrange = Color(0xFFF86030)
val LogoNavy = Color(0xFF181830)

val ErrorSoft = Color(0xFFFF8A80)

// --- Neon arcade palette (dense synthwave — not void-black) ---
val NeonVoid = Color(0xFF16082E)
val NeonAbyss = Color(0xFF22124A)
val NeonPanel = Color(0xFF35206A)
val NeonPanelHigh = Color(0xFF0E3D4E)
val NeonPanelHighest = Color(0xFF4A1A6E)
val NeonCyan = Color(0xFF00F5FF)
val NeonCyanDim = Color(0xFF2AE8F0)
val NeonCyanContainer = Color(0xFF008E9C)
val NeonCyanOnContainer = Color(0xFF041018)
val NeonMagenta = Color(0xFFFF2BD6)
val NeonMagentaDim = Color(0xFFFF5AE0)
val NeonMagentaContainer = Color(0xFFC4008C)
val NeonMagentaOnContainer = Color(0xFF1A0014)
val NeonOrange = Color(0xFFFF8A3D)
val NeonOrangeContainer = Color(0xFFE85A00)
val NeonOrangeOnContainer = Color(0xFF1A0A00)
val NeonText = Color(0xFFF2FCFF)
val NeonTextMuted = Color(0xFFB8E0F0)
val NeonOutline = Color(0xFF66F7FF)
val NeonOutlineVariant = Color(0xFFB84DFF)
val NeonError = Color(0xFFFF4D8D)
val NeonErrorContainer = Color(0xFFB00048)
val NeonOnError = Color(0xFF1A0010)
val NeonScrim = Color(0xAA100028)
val NeonInverse = Color(0xFFE8FFFF)
val NeonInversePrimary = Color(0xFF0090A0)
val NeonPlasmaA = Color(0xFF2B0B5C)
val NeonPlasmaB = Color(0xFF0C2F55)
val NeonPlasmaC = Color(0xFF4A0B58)

/**
 * Quiet interaction chrome — logo cyan, desaturated and low-opacity so focus/selection
 * reads without competing with content (and without amber).
 */
val FocusRingDefault = Color(0xFF5AA8B8)
val FocusFillDefault = LogoCyan.copy(alpha = 0.06f)
val FocusFillStrongDefault = LogoCyan.copy(alpha = 0.14f)
val SelectionIndicatorDefault = Color(0xFF6AB4C4)

/** Brand / chrome tokens that follow the active [OrglTheme] (including Neon). */
data class OrglPalette(
    val inkDeep: Color,
    val inkMid: Color,
    val inkLight: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val errorSoft: Color,
    val focusRing: Color,
    val focusFill: Color,
    val focusFillStrong: Color,
    val selectionIndicator: Color,
) {
    companion object {
        val Brand = OrglPalette(
            inkDeep = InkDeep,
            inkMid = InkMid,
            inkLight = InkLight,
            accent = AmberAccent,
            onAccent = InkDeep,
            accentSoft = AmberSoft,
            errorSoft = ErrorSoft,
            focusRing = FocusRingDefault,
            focusFill = FocusFillDefault,
            focusFillStrong = FocusFillStrongDefault,
            selectionIndicator = SelectionIndicatorDefault,
        )

        val Neon = OrglPalette(
            inkDeep = NeonPlasmaA,
            inkMid = NeonPlasmaB,
            inkLight = NeonPlasmaC,
            accent = NeonMagenta,
            onAccent = NeonText,
            accentSoft = NeonOrange,
            errorSoft = NeonError,
            focusRing = NeonCyan,
            focusFill = NeonCyan.copy(alpha = 0.22f),
            focusFillStrong = NeonMagenta.copy(alpha = 0.28f),
            selectionIndicator = NeonCyan,
        )
    }
}

val LocalOrglPalette = staticCompositionLocalOf { OrglPalette.Brand }

val FocusRing: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOrglPalette.current.focusRing

val FocusFill: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOrglPalette.current.focusFill

val FocusFillStrong: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOrglPalette.current.focusFillStrong

val SelectionIndicator: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOrglPalette.current.selectionIndicator
