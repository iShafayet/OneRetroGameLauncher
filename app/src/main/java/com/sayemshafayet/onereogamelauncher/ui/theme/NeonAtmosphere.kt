package com.sayemshafayet.onereogamelauncher.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Dense neon stage dressing: plasma gradient under the UI, CRT scanlines + grid +
 * corner blooms drawn over everything so even flat Material surfaces feel electrified.
 */
@Composable
fun NeonAtmosphere(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val gridStep = with(density) { 28.dp.toPx() }
    val bloomRadius = with(density) { 220.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                // Plasma underpaint (shows through transparent scaffolds / letterboxing).
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF2B0B5C),
                            0.28f to Color(0xFF0C2F55),
                            0.55f to Color(0xFF4A0B58),
                            0.78f to Color(0xFF0A3D4A),
                            1.0f to Color(0xFF2A0848),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 1.05f, size.height * 1.1f),
                        tileMode = TileMode.Clamp,
                    ),
                )
                // Hot corner blooms.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xAA00F5FF), Color.Transparent),
                        center = Offset(size.width * 0.08f, size.height * 0.12f),
                        radius = bloomRadius,
                    ),
                    radius = bloomRadius,
                    center = Offset(size.width * 0.08f, size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99FF2BD6), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.18f),
                        radius = bloomRadius * 0.9f,
                    ),
                    radius = bloomRadius * 0.9f,
                    center = Offset(size.width * 0.92f, size.height * 0.18f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x88FF6B2B), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 1.05f),
                        radius = bloomRadius * 1.3f,
                    ),
                    radius = bloomRadius * 1.3f,
                    center = Offset(size.width * 0.5f, size.height * 1.05f),
                )

                drawContent()

                // Horizon haze band across mid-UI.
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.42f to Color(0x2200F5FF),
                            0.55f to Color(0x33FF2BD6),
                            0.68f to Color(0x1800F5FF),
                            1.0f to Color.Transparent,
                        ),
                    ),
                )

                // Perspective grid (floor).
                val gridColor = Color(0x5522E8FF)
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.2f,
                        pathEffect = dash,
                    )
                    x += gridStep
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = gridColor.copy(alpha = 0.22f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                    y += gridStep
                }

                // CRT scanlines.
                var scan = 0f
                while (scan <= size.height) {
                    drawLine(
                        color = Color(0x14000000),
                        start = Offset(0f, scan),
                        end = Offset(size.width, scan),
                        strokeWidth = 1.5f,
                    )
                    scan += 3f
                }

                // Soft vignette so edges feel like a tube display.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x66020010)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.72f,
                    ),
                )
            },
    ) {
        content()
    }
}
