package com.sayemshafayet.onereogamelauncher.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Soft ambient stage for Calm: mist gradients, quiet orbs, feathered light —
 * peaceful density without neon aggression or flat stock surfaces.
 */
@Composable
fun CalmAtmosphere(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val bloom = with(density) { 260.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFE8F2EC),
                            0.25f to Color(0xFFDCE8F0),
                            0.5f to Color(0xFFF0EAE2),
                            0.75f to Color(0xFFE2EEE8),
                            1.0f to Color(0xFFE6EAF4),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 1.1f, size.height * 1.05f),
                        tileMode = TileMode.Clamp,
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66A8C9B8), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.2f),
                        radius = bloom,
                    ),
                    radius = bloom,
                    center = Offset(size.width * 0.15f, size.height * 0.2f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x55B8C8E0), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.28f),
                        radius = bloom * 0.95f,
                    ),
                    radius = bloom * 0.95f,
                    center = Offset(size.width * 0.88f, size.height * 0.28f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x44D4C4A8), Color.Transparent),
                        center = Offset(size.width * 0.45f, size.height * 0.92f),
                        radius = bloom * 1.2f,
                    ),
                    radius = bloom * 1.2f,
                    center = Offset(size.width * 0.45f, size.height * 0.92f),
                )
                // Soft dawn band.
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.35f to Color(0x18C5D8CC),
                            0.5f to Color(0x22D8CDB8),
                            0.65f to Color(0x14B8C8D8),
                            1.0f to Color.Transparent,
                        ),
                    ),
                )

                drawContent()

                // Feathered paper grain (very soft horizontal wash).
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = Color(0x08FFFFFF),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                    )
                    y += 5f
                }
                // Gentle edge soften.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x18A0B0A8)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.78f,
                    ),
                )
            },
    ) {
        content()
    }
}
