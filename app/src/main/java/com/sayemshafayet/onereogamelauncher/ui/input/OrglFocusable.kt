package com.sayemshafayet.onereogamelauncher.ui.input

import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.ui.theme.FocusFill
import com.sayemshafayet.onereogamelauncher.ui.theme.FocusFillStrong
import com.sayemshafayet.onereogamelauncher.ui.theme.FocusRing
import kotlinx.coroutines.delay

/** Shared IME options — avoids confusing action labels like "Execute". */
object OrglKeyboardOptions {
    val SingleLine = KeyboardOptions(imeAction = ImeAction.Done)
    val Multiline = KeyboardOptions(imeAction = ImeAction.None)
    val Search = KeyboardOptions(imeAction = ImeAction.Search)
    val Password = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
    )
}

/** Hides the soft keyboard and force-clears focus on Search / Done IME actions. */
@Composable
fun rememberOrglImeDismissActions(): KeyboardActions {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    return remember(keyboard, focusManager, view) {
        val dismiss = {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            val imm = view.context.getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            // Platform TextField may re-request IME after Done — beat that race.
            view.post {
                keyboard?.hide()
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }
            view.postDelayed({
                keyboard?.hide()
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }, 50)
            view.postDelayed({
                keyboard?.hide()
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }, 120)
        }
        KeyboardActions(
            onSearch = { dismiss() },
            onDone = { dismiss() },
            onGo = { dismiss() },
            onSend = { dismiss() },
        )
    }
}

/**
 * Soft cyan focus feedback while [interactionSource] is focused.
 * Use on Material controls that already manage focus (chips, icon buttons).
 *
 * @param drawRing when false, fill wash only (chips). List rows keep the thin ring.
 */
fun Modifier.orlgFocusChrome(
    interactionSource: MutableInteractionSource,
    cornerRadius: Dp = 8.dp,
    strokeWidth: Dp = 1.5.dp,
    drawRing: Boolean = true,
): Modifier = composed {
    val focused by interactionSource.collectIsFocusedAsState()
    if (!focused) return@composed this
    val density = LocalDensity.current
    val strokePx = with(density) { strokeWidth.toPx() }
    val cornerPx = with(density) { cornerRadius.toPx() }
    val fill = if (drawRing) FocusFill else FocusFillStrong
    val ring = FocusRing
    drawWithContent {
        drawContent()
        drawRoundRect(
            color = fill,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )
        if (drawRing) {
            drawRoundRect(
                color = ring,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = strokePx),
            )
        }
    }
}

/**
 * Makes a row/tile focusable for D-pad / gamepad navigation.
 * Touch taps still work via [clickable].
 *
 * @param showFocusRing soft cyan wash + thin ring. Grid tiles usually pass false
 * and draw their own focus chrome.
 */
fun Modifier.orlgFocusable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    showFocusRing: Boolean = true,
    gamepadXActivates: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(8.dp)

    clip(shape)
        .focusable(enabled = enabled, interactionSource = source)
        .onKeyEvent { event ->
            when {
                !enabled -> false
                gamepadXActivates && GamepadKeys.isButtonX(event) -> {
                    onClick()
                    true
                }
                GamepadKeys.isActivate(event) -> {
                    onClick()
                    true
                }
                else -> false
            }
        }
        .clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
            onClick = onClick,
        )
        .then(
            if (showFocusRing) {
                Modifier.orlgFocusChrome(source)
            } else {
                Modifier
            },
        )
}

/** FilterChip using built-in [FilterChipDefaults] container colors for focus (fill stays inside). */
@Composable
fun OrglFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        enabled = enabled,
        leadingIcon = leadingIcon,
        interactionSource = interaction,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (focused) FocusFillStrong else Color.Unspecified,
            selectedContainerColor = if (focused) {
                FocusRing.copy(alpha = 0.28f)
            } else {
                Color.Unspecified
            },
        ),
        modifier = modifier,
    )
}

/**
 * Text-field focus helpers: D-pad up/down leaves the field.
 *
 * With a gamepad, IME stays locked closed while the field is focused until A / confirm
 * unlocks it — this beats Android's showSoftInputOnFocus race. Touch focus still opens
 * the keyboard normally ([InputMode.Touch]).
 *
 * @param enabled set false while a dropdown/menu is open so D-pad stays in the menu.
 * @param openImeOnActivate when false (read-only dropdowns), A does not unlock IME.
 */
fun Modifier.orlgDpadFocusExit(
    enabled: Boolean = true,
    openImeOnActivate: Boolean = true,
): Modifier = composed {
    var imeAllowed by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val gamepadConnected = rememberGamepadConnected()
    val inputModeManager = LocalInputModeManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    fun suppressIme() {
        keyboard?.hide()
        view.context.getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Lock IME closed for gamepad/D-pad focus until A unlocks it.
    val lockIme = focused &&
        gamepadConnected &&
        !imeAllowed &&
        inputModeManager.inputMode == InputMode.Keyboard

    LaunchedEffect(lockIme) {
        if (!lockIme) return@LaunchedEffect
        repeat(12) {
            suppressIme()
            delay(32)
        }
    }

    onFocusChanged { state ->
        focused = state.isFocused
        if (!state.isFocused) {
            imeAllowed = false
        } else if (
            gamepadConnected &&
            !imeAllowed &&
            inputModeManager.inputMode == InputMode.Keyboard
        ) {
            suppressIme()
            view.post { if (!imeAllowed) suppressIme() }
            view.postDelayed({ if (!imeAllowed) suppressIme() }, 50)
            view.postDelayed({ if (!imeAllowed) suppressIme() }, 120)
        }
    }
        .onPreviewKeyEvent { event ->
            if (openImeOnActivate && GamepadKeys.isGamepadConfirm(event)) {
                imeAllowed = true
                keyboard?.show()
                val imm = view.context.getSystemService(InputMethodManager::class.java)
                val target = view.findFocus() ?: view
                imm?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
                return@onPreviewKeyEvent true
            }
            if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionDown -> {
                    imeAllowed = false
                    suppressIme()
                    focusManager.moveFocus(FocusDirection.Down)
                }
                Key.DirectionUp -> {
                    imeAllowed = false
                    suppressIme()
                    focusManager.moveFocus(FocusDirection.Up)
                }
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
