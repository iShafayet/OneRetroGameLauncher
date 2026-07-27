package com.sayemshafayet.onereogamelauncher.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.R
import com.sayemshafayet.onereogamelauncher.ui.input.OrlgInitialFocus
import com.sayemshafayet.onereogamelauncher.ui.input.orlgFocusable
import com.sayemshafayet.onereogamelauncher.ui.input.orlgListFocus
import com.sayemshafayet.onereogamelauncher.ui.input.rememberOrlgFocusRequester
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont

private const val ProjectWebsiteUrl = "https://oneretrogamelauncher.com"
private const val ProjectRepoUrl = "https://github.com/iShafayet/OneRetroGameLauncher"
private const val AuthorWebsiteUrl = "https://sayemshafayet.com"
private const val AuthorGithubUrl = "https://github.com/iShafayet"

@Composable
fun AboutScreen(
    onOpenCredits: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    val context = LocalContext.current
    val firstFocus = rememberOrlgFocusRequester()
    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.orgl_logo),
            contentDescription = "One Retro Game Launcher",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.72f)
                .widthIn(max = 280.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "One Retro Game Launcher",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BrandFont),
            )
            Text(
                "Stop scrolling your library. Start finishing it.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AboutSectionCard(title = "Project") {
            AboutLinkRow(
                icon = Icons.Default.Language,
                title = "Website",
                subtitle = "oneretrogamelauncher.com",
                onClick = { openUrl(ProjectWebsiteUrl) },
                modifier = Modifier.orlgListFocus(0, firstFocus),
            )
            HorizontalDivider(Modifier.padding(horizontal = 12.dp))
            AboutLinkRow(
                icon = Icons.Default.Code,
                title = "Source code",
                subtitle = "github.com/iShafayet/OneRetroGameLauncher",
                onClick = { openUrl(ProjectRepoUrl) },
            )
        }

        AboutSectionCard(title = "Mission") {
            AboutBody(
                "Help you play one game at a time. Pick something, commit to it, and give it a fair run — " +
                    "instead of drowning in an endless “maybe later” shelf.",
            )
        }

        AboutSectionCard(title = "Vision") {
            AboutBody(
                "A calm library that stays out of the way: your ROMs stay yours, artwork lives in your ORGL data folder, " +
                    "and emulators stay external. Optional integrations (ES-DE media, ScreenScraper, RetroAchievements, " +
                    "HowLongToBeat) enrich the experience without locking you in.",
            )
        }

        AboutSectionCard(title = "How we work") {
            AboutBody(
                "• Setup mode — organize systems, scrape media, tune emulators.\n" +
                    "• Play mode — commit to a single game until you finish or drop it.\n" +
                    "• Your folders — ROMs are read-only; scrapes write only to ORGL data.\n" +
                    "• Your choice of emulator — RetroArch or standalone, per system or per game.",
            )
        }

        AboutSectionCard(title = "Author") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Sayem Shafayet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "FOSS engineer and maintainer. Builds free software that keeps data in your hands — " +
                            "including libre.money and nkrypt.xyz — and ORGL continues that same open ethos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 12.dp))
            AboutLinkRow(
                icon = Icons.Default.Language,
                title = "Website",
                subtitle = "sayemshafayet.com",
                onClick = { openUrl(AuthorWebsiteUrl) },
            )
            HorizontalDivider(Modifier.padding(horizontal = 12.dp))
            AboutLinkRow(
                icon = Icons.Default.Code,
                title = "GitHub",
                subtitle = "github.com/iShafayet",
                onClick = { openUrl(AuthorGithubUrl) },
            )
        }

        AboutSectionCard(title = "Legal") {
            AboutLinkRow(
                icon = Icons.Default.Language,
                title = "Privacy Policy",
                subtitle = "How ORGL handles your data",
                onClick = onOpenPrivacy,
            )
            HorizontalDivider(Modifier.padding(horizontal = 12.dp))
            AboutLinkRow(
                icon = Icons.Default.Language,
                title = "Terms of Service",
                subtitle = "Rules for using ORGL",
                onClick = onOpenTerms,
            )
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onOpenCredits,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Credits")
        }
    }
    OrlgInitialFocus(firstFocus)
}

@Composable
private fun AboutSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = BrandFont),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun AboutBody(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .orlgFocusable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Opens in browser",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
