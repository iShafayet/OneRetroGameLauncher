package com.sayemshafayet.onereogamelauncher.ui.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object GamepadKeys {
    private val activateKeys = setOf(
        Key.Enter,
        Key.NumPadEnter,
        Key.DirectionCenter,
        Key.Spacebar,
        Key.ButtonA,
        Key.ButtonStart,
    )

    private val gamepadBackKeys = setOf(
        Key.ButtonB,
    )

    private val systemBackKeys = setOf(
        Key.Escape,
        Key.Back,
    )

    private val shoulderLeftKeys = setOf(
        Key.ButtonL1,
        Key.PageUp,
    )

    private val shoulderRightKeys = setOf(
        Key.ButtonR1,
        Key.PageDown,
    )

    fun isActivate(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in activateKeys

    /**
     * Face-button / D-pad center confirm only — excludes Enter/Space so IME Done
     * does not re-open the soft keyboard on text fields.
     */
    fun isGamepadConfirm(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp &&
            event.key in setOf(Key.ButtonA, Key.DirectionCenter, Key.ButtonStart)

    fun isGamepadBack(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in gamepadBackKeys

    fun isSystemBack(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in systemBackKeys

    fun isAbout(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key == Key.ButtonX

    /** X — library search on Library hub; Play on Focus; otherwise unhandled. */
    fun isButtonX(event: KeyEvent): Boolean = isAbout(event)

    /** Y — mode toggle on root pages (double-press); otherwise consumed. */
    fun isModeToggle(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key == Key.ButtonY

    fun isShoulderLeft(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in shoulderLeftKeys

    fun isShoulderRight(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in shoulderRightKeys
}

fun hasGamepadConnected(context: Context): Boolean {
    val inputManager = context.getSystemService(InputManager::class.java) ?: return false
    return inputManager.inputDeviceIds.any { deviceId ->
        val device = InputDevice.getDevice(deviceId) ?: return@any false
        val sources = device.sources
        (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }
}

@Composable
fun rememberGamepadConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(hasGamepadConnected(context)) }
    DisposableEffect(context) {
        val inputManager = context.getSystemService(InputManager::class.java)
        if (inputManager == null) {
            onDispose {}
        } else {
            val listener = object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    connected = hasGamepadConnected(context)
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    connected = hasGamepadConnected(context)
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    connected = hasGamepadConnected(context)
                }
            }
            inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
            connected = hasGamepadConnected(context)
            onDispose { inputManager.unregisterInputDeviceListener(listener) }
        }
    }
    return connected
}

/**
 * Handles back / B: dismisses the soft keyboard first when it is open; otherwise pops
 * [onBack] when [canPopBack], or runs [onRootBack] when provided (e.g. double-press exit).
 */
@Composable
fun GamepadBackHandler(
    canPopBack: Boolean,
    onBack: () -> Unit,
    onRootBack: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    BackHandler {
        if (view.isSoftKeyboardVisible()) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            view.post { keyboard?.hide() }
            view.postDelayed({ keyboard?.hide() }, 80)
            return@BackHandler
        }
        when {
            canPopBack -> onBack()
            onRootBack != null -> onRootBack()
        }
    }
}

/** True when the IME / soft keyboard is currently visible. */
fun View.isSoftKeyboardVisible(): Boolean =
    ViewCompat.getRootWindowInsets(this)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
