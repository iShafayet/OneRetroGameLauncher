package com.sayemshafayet.onereogamelauncher.ui.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
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

    fun isGamepadBack(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in gamepadBackKeys

    fun isSystemBack(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key in systemBackKeys

    fun isAbout(event: KeyEvent): Boolean =
        event.type == KeyEventType.KeyUp && event.key == Key.ButtonX

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
 * Handles back / B: pops [onBack] when [canPopBack]; otherwise runs [onRootBack] when provided
 * (e.g. double-press to exit at a mode root).
 */
@Composable
fun GamepadBackHandler(
    canPopBack: Boolean,
    onBack: () -> Unit,
    onRootBack: (() -> Unit)? = null,
) {
    BackHandler {
        when {
            canPopBack -> onBack()
            onRootBack != null -> onRootBack()
        }
    }
}
