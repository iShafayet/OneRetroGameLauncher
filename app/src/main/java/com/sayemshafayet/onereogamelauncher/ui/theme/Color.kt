package com.sayemshafayet.onereogamelauncher.ui.theme

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

/**
 * Quiet interaction chrome — logo cyan, desaturated and low-opacity so focus/selection
 * reads without competing with content (and without amber).
 */
val FocusRing = Color(0xFF5AA8B8)
val FocusFill = LogoCyan.copy(alpha = 0.06f)
/** Stronger wash when focus is fill-only (chips) — no border. */
val FocusFillStrong = LogoCyan.copy(alpha = 0.14f)
val SelectionIndicator = Color(0xFF6AB4C4)
