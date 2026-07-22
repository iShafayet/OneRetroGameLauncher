package com.sayemshafayet.onereogamelauncher.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester

@Composable
fun PlayToSetupGuardDialog(
    activeRunCount: Int = 1,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val actionFocus = rememberOrlgFocusRequester()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (activeRunCount > 1) "Heads up — you have active runs" else "Heads up — you're on a run",
            )
        },
        text = {
            Column {
                Text(
                    if (activeRunCount > 1) {
                        "You have $activeRunCount games locked in Play mode. Setup is for maintenance — " +
                            "organize your library, tweak settings, scrape artwork. " +
                            "Don't browse for something else to play."
                    } else {
                        "You have a game locked in Play mode. Setup is for maintenance — " +
                            "organize your library, tweak settings, scrape artwork. " +
                            "Don't browse for something else to play."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Be on your best behavior. ORGL trusts you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmed,
                modifier = Modifier.focusRequester(actionFocus),
            ) {
                Text("Enter Setup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay in Play")
            }
        },
    )

    OrlgInitialFocus(actionFocus)
}
