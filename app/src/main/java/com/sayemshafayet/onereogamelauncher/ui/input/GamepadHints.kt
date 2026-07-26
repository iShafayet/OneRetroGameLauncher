package com.sayemshafayet.onereogamelauncher.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Console-style face/shoulder button prompt (letter or short label in a circle/pill).
 */
@Composable
fun GamepadHintBadge(
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val bg = MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = MaterialTheme.colorScheme.onSurface
    val border = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val isWide = label.length > 1
    val minSize = if (compact) 16.dp else 18.dp

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = if (isWide) minSize + 8.dp else minSize, minHeight = minSize)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = if (isWide) 5.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 9.sp else 10.sp,
            lineHeight = if (compact) 10.sp else 11.sp,
            maxLines = 1,
        )
    }
}

/** Corner overlay for icon buttons / CTAs (e.g. X on Library search, A on focused Launch). */
@Composable
fun GamepadHintOverlay(
    label: String,
    modifier: Modifier = Modifier,
    offsetX: Dp = 2.dp,
    offsetY: Dp = (-4).dp,
) {
    GamepadHintBadge(
        label = label,
        compact = true,
        modifier = modifier.offset(x = offsetX, y = offsetY),
    )
}

@Composable
fun GamepadShoulderHints(
    modifier: Modifier = Modifier,
    left: String = "L1",
    right: String = "R1",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GamepadHintBadge(label = left, compact = true)
        Spacer(modifier = Modifier.weight(1f))
        GamepadHintBadge(label = right, compact = true)
    }
}

/** True when a physical gamepad/joystick is connected — used to hide hints on touch-only devices. */
@Composable
fun rememberShowGamepadHints(): Boolean = rememberGamepadConnected()
