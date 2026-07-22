package com.sayemshafayet.onereogamelauncher.ui.input

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp

/**
 * Makes a row/tile focusable for D-pad / gamepad navigation with a visible focus ring.
 * Touch taps still work via [clickable].
 */
fun Modifier.orlgFocusable(
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val ringColor = MaterialTheme.colorScheme.primary

    clip(shape)
        .focusable(enabled = enabled, interactionSource = interactionSource)
        .onKeyEvent { event ->
            if (enabled && GamepadKeys.isActivate(event)) {
                onClick()
                true
            } else {
                false
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
        .then(
            if (focused) {
                Modifier.border(2.dp, ringColor, shape)
            } else {
                Modifier
            },
        )
}

fun Modifier.orlgListFocus(index: Int, firstItemFocus: FocusRequester): Modifier =
    if (index == 0) focusRequester(firstItemFocus) else this

@Composable
fun OrlgInitialFocus(
    focusRequester: FocusRequester,
    enabled: Boolean = true,
    resetKey: Any? = Unit,
) {
    val gamepadConnected = rememberGamepadConnected()
    LaunchedEffect(gamepadConnected, enabled, resetKey) {
        if (gamepadConnected && enabled) {
            runCatching { focusRequester.requestFocus() }
        }
    }
}

@Composable
fun rememberOrlgFocusRequester(): FocusRequester = remember { FocusRequester() }
