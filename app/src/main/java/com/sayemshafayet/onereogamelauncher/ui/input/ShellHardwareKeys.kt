package com.sayemshafayet.onereogamelauncher.ui.input

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Activity-level hardware key dispatch so shoulder / face buttons keep working when Compose
 * focus is cleared or key preview chains miss the focused screen.
 *
 * [ModeShell] registers defaults; Play screens register screen-specific X handlers while mounted.
 * [MainActivity] forwards matching key events.
 */
object ShellHardwareKeys {
    @Volatile
    var shoulderHandler: ((KeyEvent) -> Boolean)? = null

    @Volatile
    var defaultButtonXHandler: ((KeyEvent) -> Boolean)? = null

    @Volatile
    var screenButtonXHandler: ((KeyEvent) -> Boolean)? = null

    fun isShoulderKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
            keyCode == KeyEvent.KEYCODE_BUTTON_R1 ||
            keyCode == KeyEvent.KEYCODE_PAGE_UP ||
            keyCode == KeyEvent.KEYCODE_PAGE_DOWN

    fun isShoulderLeft(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_PAGE_UP

    fun isShoulderRight(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_PAGE_DOWN

    fun isButtonX(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_BUTTON_X

    fun hasButtonXHandler(): Boolean =
        screenButtonXHandler != null || defaultButtonXHandler != null

    /** Consume down+up when a handler is registered; invoke handlers on up only. */
    fun dispatchButtonX(event: KeyEvent): Boolean {
        if (!isButtonX(event.keyCode) || !hasButtonXHandler()) return false
        if (event.action == KeyEvent.ACTION_UP) {
            screenButtonXHandler?.invoke(event)?.let { if (it) return true }
            return defaultButtonXHandler?.invoke(event) == true
        }
        return true
    }
}

/** Register [handler] as the screen-level X action while this composable is in the tree. */
@Composable
fun RegisterScreenButtonXHandler(handler: (KeyEvent) -> Boolean) {
    val current by rememberUpdatedState(handler)
    DisposableEffect(Unit) {
        val registered: (KeyEvent) -> Boolean = { event -> current(event) }
        ShellHardwareKeys.screenButtonXHandler = registered
        onDispose {
            if (ShellHardwareKeys.screenButtonXHandler === registered) {
                ShellHardwareKeys.screenButtonXHandler = null
            }
        }
    }
}
