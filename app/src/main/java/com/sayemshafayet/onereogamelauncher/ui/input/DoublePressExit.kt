package com.sayemshafayet.onereogamelauncher.ui.input

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private const val CONFIRM_WINDOW_MS = 2_000L

/**
 * First press shows [message]; second press within 2s runs [onConfirm].
 * Used for exit and mode toggle on root screens.
 */
@Composable
fun rememberDoublePressConfirmHandler(
    resetKey: Any?,
    message: String,
    onConfirm: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val lastPressMs = remember { mutableLongStateOf(0L) }
    val currentMessage by rememberUpdatedState(message)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    LaunchedEffect(resetKey) {
        lastPressMs.longValue = 0L
    }
    return remember {
        {
            val now = SystemClock.elapsedRealtime()
            if (now - lastPressMs.longValue <= CONFIRM_WINDOW_MS) {
                lastPressMs.longValue = 0L
                currentOnConfirm()
            } else {
                lastPressMs.longValue = now
                Toast.makeText(context, currentMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun rememberDoublePressExitHandler(
    resetKey: Any?,
    message: String,
    onExit: () -> Unit,
): () -> Unit = rememberDoublePressConfirmHandler(
    resetKey = resetKey,
    message = message,
    onConfirm = onExit,
)
