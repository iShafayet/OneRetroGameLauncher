package com.sayemshafayet.onereogamelauncher.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

private enum class SetupGuardStep {
    WARNING,
    COUNTDOWN,
    CONFIRM,
}

@Composable
fun PlayToSetupGuardDialog(
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
) {
    var step by remember { mutableStateOf(SetupGuardStep.WARNING) }
    var countdown by remember { mutableIntStateOf(3) }
    val actionFocus = rememberOrlgFocusRequester()

    LaunchedEffect(step) {
        if (step == SetupGuardStep.COUNTDOWN) {
            for (remaining in 3 downTo 1) {
                countdown = remaining
                delay(1_000)
            }
            step = SetupGuardStep.CONFIRM
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Heads up — you're on a run") },
        text = {
            Column {
                Text(
                    "You have a game locked in Play mode. Setup is for maintenance only — " +
                        "organize your library, tweak settings, scrape artwork. " +
                        "Don't browse for something else to play.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Be on your best behavior. ORGL trusts you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                when (step) {
                    SetupGuardStep.WARNING -> Unit
                    SetupGuardStep.COUNTDOWN -> {
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
                    SetupGuardStep.CONFIRM -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Ready when you are. Tap below to enter Setup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                SetupGuardStep.WARNING -> {
                    Button(
                        onClick = { step = SetupGuardStep.COUNTDOWN },
                        modifier = Modifier
                            .focusRequester(actionFocus)
                            .padding(end = 8.dp),
                    ) {
                        Text("I understand")
                    }
                }
                SetupGuardStep.COUNTDOWN -> {
                    Button(onClick = {}, enabled = false) {
                        Text("Wait…")
                    }
                }
                SetupGuardStep.CONFIRM -> {
                    Button(
                        onClick = onConfirmed,
                        modifier = Modifier.focusRequester(actionFocus),
                    ) {
                        Text("Enter Setup")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay in Play")
            }
        },
    )

    OrlgInitialFocus(actionFocus, enabled = step != SetupGuardStep.COUNTDOWN)
}
