package com.sayemshafayet.onereogamelauncher.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import kotlinx.coroutines.delay

private enum class DebugLaunchStep {
    WARNING,
    COUNTDOWN,
    CONFIRM,
}

@Composable
fun SetupDebugLaunchDialog(
    gameTitle: String,
    activeRunCount: Int,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
) {
    var step by remember { mutableStateOf(DebugLaunchStep.WARNING) }
    var countdown by remember { mutableIntStateOf(3) }
    val actionFocus = rememberOrlgFocusRequester()

    LaunchedEffect(step) {
        if (step == DebugLaunchStep.COUNTDOWN) {
            for (remaining in 3 downTo 1) {
                countdown = remaining
                delay(1_000)
            }
            step = DebugLaunchStep.CONFIRM
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug launch") },
        text = {
            Column {
                Text(
                    buildString {
                        append("You're about to launch ")
                        append(gameTitle)
                        append(" from Setup while ")
                        if (activeRunCount > 1) {
                            append("$activeRunCount Play slots are active.")
                        } else {
                            append("a Play slot is active.")
                        }
                        append(" This is only for debugging and testing — not for starting a new play run.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Use Play mode to commit to a game properly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                when (step) {
                    DebugLaunchStep.WARNING -> Unit
                    DebugLaunchStep.COUNTDOWN -> {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            countdown.toString(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Take a breath…",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DebugLaunchStep.CONFIRM -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Ready when you are. Tap below to launch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                DebugLaunchStep.WARNING -> {
                    Button(
                        onClick = { step = DebugLaunchStep.COUNTDOWN },
                        modifier = Modifier.focusRequester(actionFocus),
                    ) {
                        Text("I understand")
                    }
                }
                DebugLaunchStep.COUNTDOWN -> {
                    Button(onClick = {}, enabled = false) {
                        Text("Wait…")
                    }
                }
                DebugLaunchStep.CONFIRM -> {
                    Button(
                        onClick = onConfirmed,
                        modifier = Modifier.focusRequester(actionFocus),
                    ) {
                        Text("Launch anyway")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    OrlgInitialFocus(actionFocus, enabled = step != DebugLaunchStep.COUNTDOWN)
}
