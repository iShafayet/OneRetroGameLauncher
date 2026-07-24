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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

/**
 * Makes a row/tile focusable for D-pad / gamepad navigation.
 * Touch taps still work via [clickable].
 *
 * @param showFocusRing thin Material primary ring (fine for list rows). Grid tiles
 * usually pass false and draw their own focus chrome.
 */
fun Modifier.orlgFocusable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    showFocusRing: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val ringColor = MaterialTheme.colorScheme.primary

    clip(shape)
        .focusable(enabled = enabled, interactionSource = source)
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
            interactionSource = source,
            indication = null,
            onClick = onClick,
        )
        .then(
            if (showFocusRing && focused) {
                Modifier.border(2.dp, ringColor, shape)
            } else {
                Modifier
            },
        )
}

/**
 * On search / text fields, D-pad up/down leaves the field and focuses the next control.
 * Left/right stay with the field for caret movement.
 *
 * @param enabled set false while a dropdown/menu is open so D-pad stays in the menu.
 */
fun Modifier.orlgDpadFocusExit(enabled: Boolean = true): Modifier = composed {
    val focusManager = LocalFocusManager.current
    onPreviewKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
            Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
            else -> false
        }
    }
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
