package com.sayemshafayet.onereogamelauncher.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.legal.LegalDocumentKind

@Composable
fun LegalDocumentScreen(
    document: LegalDocumentKind,
    onBack: () -> Unit,
    onOpenLinkedDocument: ((LegalDocumentKind) -> Unit)? = null,
) {
    val context = LocalContext.current

    LegalDocumentSurface {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            when (document) {
                LegalDocumentKind.Privacy -> PrivacyPolicyContent()
                LegalDocumentKind.Terms -> TermsOfServiceContent(
                    onOpenPrivacy = onOpenLinkedDocument?.let { open ->
                        { open(LegalDocumentKind.Privacy) }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(document.webUrl)))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open on website")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
