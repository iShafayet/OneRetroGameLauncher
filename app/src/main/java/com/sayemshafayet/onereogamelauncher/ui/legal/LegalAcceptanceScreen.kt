package com.sayemshafayet.onereogamelauncher.ui.legal

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.legal.LegalDocumentKind
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.theme.InkDeep
import com.sayemshafayet.onereogamelauncher.ui.theme.InkLight
import com.sayemshafayet.onereogamelauncher.ui.theme.InkMid
import com.sayemshafayet.onereogamelauncher.ui.theme.Mist

@Composable
fun LegalAcceptanceScreen(
    onAccepted: () -> Unit,
    onOpenDocument: (LegalDocumentKind) -> Unit,
) {
    val activity = LocalActivity.current
    var agreed by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(InkDeep, InkMid, InkLight)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Text(
            "ORGL",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = BrandFont),
            color = AmberAccent,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Terms & Privacy",
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
            color = Mist,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Before you continue, please read and accept our Terms of Service and Privacy Policy.",
            style = MaterialTheme.typography.bodyLarge,
            color = Mist.copy(alpha = 0.88f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "ORGL includes no analytics or telemetry. Optional integrations (RetroAchievements, " +
                "ScreenScraper, HowLongToBeat) may contact third-party services under their own policies.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mist.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = { onOpenDocument(LegalDocumentKind.Terms) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Read Terms of Service", color = Mist)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onOpenDocument(LegalDocumentKind.Privacy) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Read Privacy Policy", color = Mist)
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = AmberAccent,
                    uncheckedColor = Mist.copy(alpha = 0.6f),
                    checkmarkColor = InkDeep,
                ),
            )
            Text(
                "I agree to the Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAccepted,
            enabled = agreed,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmberAccent,
                contentColor = InkDeep,
                disabledContainerColor = AmberAccent.copy(alpha = 0.35f),
                disabledContentColor = InkDeep.copy(alpha = 0.5f),
            ),
        ) {
            Text("Agree and continue")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Decline and exit", color = Mist.copy(alpha = 0.85f))
        }
    }
}
