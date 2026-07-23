package com.sayemshafayet.onereogamelauncher.ui.input

import android.view.KeyEvent

/**
 * Activity-level shoulder key dispatch so L1/R1 keep working when Compose focus is cleared
 * (e.g. after tapping the non-focusable bottom nav, or on empty lists).
 *
 * [ModeShell] registers a handler; [MainActivity] forwards matching key events.
 */
object ShellHardwareKeys {
    @Volatile
    var handler: ((KeyEvent) -> Boolean)? = null

    fun isShoulderKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
            keyCode == KeyEvent.KEYCODE_BUTTON_R1 ||
            keyCode == KeyEvent.KEYCODE_PAGE_UP ||
            keyCode == KeyEvent.KEYCODE_PAGE_DOWN

    fun isShoulderLeft(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_PAGE_UP

    fun isShoulderRight(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_BUTTON_R1 || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
}
