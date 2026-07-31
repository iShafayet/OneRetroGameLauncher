package com.sayemshafayet.onereogamelauncher.ui.onboarding.beginner

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.data.homebrew.LegalRomsGuideLink
import com.sayemshafayet.onereogamelauncher.data.orgl.OrglDataDirectory
import com.sayemshafayet.onereogamelauncher.domain.BeginnerLibraryPreview
import com.sayemshafayet.onereogamelauncher.domain.FreeHomebrewGame
import com.sayemshafayet.onereogamelauncher.domain.RomsStructureCheck
import com.sayemshafayet.onereogamelauncher.library.RomsRootStructureChecker
import com.sayemshafayet.onereogamelauncher.ui.theme.AmberAccent
import com.sayemshafayet.onereogamelauncher.ui.theme.BrandFont
import com.sayemshafayet.onereogamelauncher.ui.theme.Mist

@Composable
fun BeginnerSectionLabel() {
    Text(
        "Beginner setup",
        style = MaterialTheme.typography.labelLarge.copy(fontFamily = BrandFont),
        color = AmberAccent,
    )
}

@Composable
fun BeginnerOrglScreen(
    uri: String?,
    pathHint: String?,
    preselected: Boolean,
    reused: Boolean,
    error: String?,
    conflictError: String?,
    onPick: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "A home for ORGL’s files",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Before we touch games or emulators, pick a folder just for ORGL. " +
            "This is where we’ll keep artwork we download, optional saved logins, and other app data.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "This is not a ROMs folder. Think of it as ORGL’s notebook — separate from the games themselves. " +
            "A good choice is an empty folder named ORGL-Data next to where you’ll keep ROMs later.",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.75f),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "We’ll place a tiny ${OrglDataDirectory.META_FILE_NAME} marker inside (spec v${OrglDataDirectory.SPEC_VERSION}) " +
            "so future installs recognize the folder.",
        style = MaterialTheme.typography.bodySmall,
        color = Mist.copy(alpha = 0.65f),
    )
    if (preselected) {
        Spacer(Modifier.height(12.dp))
        Text(
            "We still have access to a folder you linked before — confirm it or pick another.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(if (uri == null) "Choose ORGL data folder" else "Change folder", color = Mist)
    }
    BeginnerFolderStatus(pathHint = pathHint, uri = uri)
    if (uri != null && reused) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Existing ORGL data detected — we’ll reuse what’s already here.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    conflictError?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
    error?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BeginnerHaveRomsScreen(
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Do you already have some ROMs?",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "ROMs are the game files themselves. If you already dumped or downloaded legal copies, " +
            "we can point ORGL at them. If not, we’ll help you get started the right way.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    Spacer(Modifier.height(28.dp))
    BeginnerChoiceCard(
        title = "Yes — I have ROMs",
        body = "I’ll choose the folder where my game files live.",
        onClick = onYes,
    )
    Spacer(Modifier.height(16.dp))
    BeginnerChoiceCard(
        title = "Not yet",
        body = "Show me how to get legal ROMs, or download a few legal free starter games.",
        onClick = onNo,
    )
}

@Composable
fun BeginnerRomsSetupScreen(
    uri: String?,
    pathHint: String?,
    preselected: Boolean,
    structureChecking: Boolean,
    structureCheck: RomsStructureCheck?,
    structureError: String?,
    scanning: Boolean,
    scanError: String?,
    onPick: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Your ROMs folder",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Pick the root folder that contains system subfolders — not a single game file. " +
            "ORGL expects one folder per system, the same layout ES-DE uses.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    if (preselected) {
        Spacer(Modifier.height(12.dp))
        Text(
            "We still have access to a folder you linked before — confirm it or pick another.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmberAccent,
        )
    }
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
        Text(if (uri == null) "Choose ROMs folder" else "Change folder", color = Mist)
    }
    BeginnerFolderStatus(pathHint = pathHint, uri = uri)

    when {
        structureChecking -> {
            Spacer(Modifier.height(20.dp))
            Text(
                "Checking folder layout…",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
            )
        }
        structureCheck != null && !structureCheck.isValid -> {
            Spacer(Modifier.height(20.dp))
            Text(
                "That folder doesn’t look like a ROMs root yet",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = BrandFont),
                color = AmberAccent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                structureError
                    ?: "We need immediate child folders named after systems.",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Example layout:",
                style = MaterialTheme.typography.labelLarge,
                color = AmberAccent,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                buildString {
                    appendLine("ROMs/")
                    RomsRootStructureChecker.EXAMPLE_FOLDERS.take(6).forEach { folder ->
                        appendLine("  $folder/")
                        appendLine("    YourGame.rom")
                    }
                }.trimEnd(),
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.8f),
            )
            if (structureCheck.childDirectoryNames.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Folders we found here: ${structureCheck.childDirectoryNames.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Mist.copy(alpha = 0.65f),
                )
            }
        }
        structureCheck != null && structureCheck.isValid -> {
            Spacer(Modifier.height(16.dp))
            Text(
                buildString {
                    append("Looks good — found ")
                    append(structureCheck.matchedFolders.size)
                    append(if (structureCheck.matchedFolders.size == 1) " system folder: " else " system folders: ")
                    append(structureCheck.matchedFolders.joinToString())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AmberAccent,
            )
            if (scanning) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Scanning for games…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist.copy(alpha = 0.85f),
                )
            }
        }
    }
    scanError?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BeginnerRomsSummaryScreen(
    scanning: Boolean,
    preview: BeginnerLibraryPreview?,
    scanError: String?,
    onGetFreeGames: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        when {
            scanning -> "Scanning your games"
            preview != null && preview.gamesFound > 0 -> "Here’s what we found"
            else -> "No games found yet"
        },
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    when {
        scanning -> {
            Text(
                "Looking through your system folders…",
                style = MaterialTheme.typography.bodyLarge,
                color = Mist.copy(alpha = 0.88f),
            )
        }
        preview != null && preview.gamesFound > 0 -> {
            Text(
                "${preview.gamesFound} game" +
                    (if (preview.gamesFound == 1) "" else "s") +
                    " across ${preview.systems.size} system" +
                    if (preview.systems.size == 1) "." else "s.",
                style = MaterialTheme.typography.bodyLarge,
                color = Mist.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(20.dp))
            preview.systems.forEach { system ->
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(
                        "${system.displayName} (${system.folderName})",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = BrandFont),
                        color = AmberAccent,
                    )
                    Spacer(Modifier.height(4.dp))
                    system.sampleTitles.forEach { title ->
                        Text(
                            "· $title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Mist.copy(alpha = 0.88f),
                        )
                    }
                    val remaining = system.gameCount - system.sampleTitles.size
                    if (remaining > 0) {
                        Text(
                            "· and $remaining more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Mist.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
        else -> {
            Text(
                "We couldn’t find playable games in the system folders. " +
                    "Double-check file extensions, or grab a few legal free starter games below.",
                style = MaterialTheme.typography.bodyLarge,
                color = Mist.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(20.dp))
            BeginnerChoiceCard(
                title = "Get legal free starter games",
                body = "Download a small curated set of games that are legal and free to share.",
                onClick = onGetFreeGames,
            )
        }
    }
    scanError?.let {
        Spacer(Modifier.height(12.dp))
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BeginnerNoRomsHelpScreen(
    guides: List<LegalRomsGuideLink>,
    onOpenGuide: (String) -> Unit,
    onGetFreeGames: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Getting ROMs the right way",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "ORGL never ships commercial ROMs. The legal paths are roughly:",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "· Dump games you personally own from your own cartridges or discs\n" +
            "· Download titles the copyright holder has made free to share\n" +
            "· Avoid “ROM sites” that redistribute commercial games without permission",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.82f),
    )
    Spacer(Modifier.height(16.dp))
    Text(
        "Helpful guides",
        style = MaterialTheme.typography.labelLarge,
        color = AmberAccent,
    )
    Spacer(Modifier.height(8.dp))
    guides.forEach { guide ->
        OutlinedButton(
            onClick = { onOpenGuide(guide.url) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(guide.title, color = Mist)
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(20.dp))
    Text(
        "Or start immediately",
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "We can download a tiny pack of legal free games for you " +
            "(gratis + libre / redistributable).",
        style = MaterialTheme.typography.bodyMedium,
        color = Mist.copy(alpha = 0.82f),
    )
    Spacer(Modifier.height(16.dp))
    BeginnerChoiceCard(
        title = "Download legal free starter games",
        body = "You’ll pick a ROMs folder, then we’ll fetch the curated pack.",
        onClick = onGetFreeGames,
    )
}

@Composable
fun BeginnerFreeGamesScreen(
    romsUri: String?,
    romsPath: String?,
    games: List<FreeHomebrewGame>,
    downloading: Boolean,
    status: String?,
    error: String?,
    downloaded: Boolean,
    onPickRoms: () -> Unit,
    onDownload: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Legal free starter games",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "These titles are legal and free to download and redistribute. We’ll save them into the correct " +
            "system folders under your ROMs root (for example gba/).",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
    Spacer(Modifier.height(20.dp))
    games.forEach { game ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Mist.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Text(
                game.title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = BrandFont),
                color = AmberAccent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${game.systemFolder}/ · ${game.blurb}",
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "ROMs folder",
        style = MaterialTheme.typography.labelLarge,
        color = AmberAccent,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onPickRoms, modifier = Modifier.fillMaxWidth()) {
        Text(if (romsUri == null) "Choose ROMs folder" else "Change ROMs folder", color = Mist)
    }
    BeginnerFolderStatus(pathHint = romsPath, uri = romsUri)
    Spacer(Modifier.height(16.dp))
    status?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = AmberAccent)
        Spacer(Modifier.height(8.dp))
    }
    error?.let {
        Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
    }
    if (downloaded && !downloading) {
        Text(
            "Downloads finished. We’ll take you to your library summary next.",
            style = MaterialTheme.typography.bodyMedium,
            color = Mist.copy(alpha = 0.85f),
        )
    }
}

@Composable
fun BeginnerComingSoonScreen() {
    Spacer(Modifier.height(24.dp))
    Text(
        "Great progress",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = BrandFont),
        color = Mist,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "You’ve got a place for ORGL’s data and at least one game in your library. " +
            "Next we’ll help you install an emulator and finish setup — that part is coming soon.",
        style = MaterialTheme.typography.bodyLarge,
        color = Mist.copy(alpha = 0.88f),
    )
}

@Composable
private fun BeginnerChoiceCard(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Mist.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = BrandFont),
            color = AmberAccent,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = Mist.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun BeginnerFolderStatus(
    pathHint: String?,
    uri: String?,
) {
    if (pathHint != null) {
        Spacer(Modifier.height(12.dp))
        Text("Resolved path", color = AmberAccent, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(pathHint, style = MaterialTheme.typography.bodyMedium, color = Mist.copy(alpha = 0.85f))
    } else if (uri != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Folder linked via SAF.",
            style = MaterialTheme.typography.bodySmall,
            color = Mist.copy(alpha = 0.7f),
        )
    }
}
