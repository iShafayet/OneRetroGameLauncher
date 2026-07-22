package com.sayemshafayet.onereogamelauncher.ui.input

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val EXIT_CONFIRM_WINDOW_MS = 2_000L

@Composable
fun rememberDoublePressExitHandler(
    resetKey: Any?,
    message: String,
    onExit: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val lastPressMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(resetKey) {
        lastPressMs.longValue = 0L
    }
    return remember(message, onExit) {
        {
            val now = SystemClock.elapsedRealtime()
            if (now - lastPressMs.longValue <= EXIT_CONFIRM_WINDOW_MS) {
                lastPressMs.longValue = 0L
                onExit()
            } else {
                lastPressMs.longValue = now
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
