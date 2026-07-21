package com.sayemshafayet.onereogamelauncher.launch

import com.sayemshafayet.onereogamelauncher.library.DiscDescriptorPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StandaloneRomPathsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun resolve_prefersReadableFilesystemPath() {
        val file = File.createTempFile("game", ".iso")
        file.deleteOnExit()
        val resolved = StandaloneRomPaths.resolve(
            romPath = file.absolutePath,
            romPathsJson = "[]",
            romsTreeUri = null,
        )
        assertEquals(file.absolutePath, resolved.romExtra)
    }

    @Test
    fun resolve_contentUriIncludesGrantHints() {
        val contentUri =
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments/document/primary%3ADocuments%2Fps2%2Fgame.iso"
        val resolved = StandaloneRomPaths.resolve(
            romPath = contentUri,
            romPathsJson = "[]",
            romsTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
            romsDirPath = null,
        )
        assertEquals(contentUri, resolved.romExtra)
        assertNotNull(resolved.grantDocumentUri)
        assertNotNull(resolved.grantTreeUri)
    }

    @Test
    fun resolve_cueFromSafBootsBinContentUriLikeEsDe() {
        // ES-DE ps2 extensions omit .cue — game entry is the .bin with %ROMSAF%.
        val cueUri =
            "content://com.android.externalstorage.documents/tree/primary%3AROMs/" +
                "document/primary%3AROMs%2Fps2%2FVirtua%20Cop%2FVirtua%20Cop.cue"
        val resolved = StandaloneRomPaths.resolve(
            romPath = cueUri,
            romPathsJson = """["Virtua Cop/Virtua Cop.cue","Virtua Cop/Virtua Cop.bin"]""",
            romsTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AROMs",
            romsDirPath = null,
            systemFolder = "ps2",
        )
        assertTrue(
            "Expected bin content URI as bootPath (ES-DE style), got: ${resolved.romExtra}",
            resolved.romExtra.contains("Virtua%20Cop.bin", ignoreCase = true),
        )
        assertTrue(resolved.romExtra.startsWith("content:", ignoreCase = true))
        assertTrue(resolved.grantDocumentUris.contains(cueUri))
        assertTrue(resolved.grantDocumentUris.any { it.contains("Virtua%20Cop.bin", ignoreCase = true) })
    }

    @Test
    fun resolve_cueUsesFilesystemBinWhenNoContentUri() {
        val root = tempFolder.newFolder("roms")
        val gameDir = File(root, "ps2/Virtua Cop").apply { mkdirs() }
        val bin = File(gameDir, "Virtua Cop.bin").apply { writeBytes(byteArrayOf(0)) }
        val cue = File(gameDir, "Virtua Cop.cue").apply {
            writeText(
                """
                FILE "${bin.name}" BINARY
                  TRACK 01 MODE2/2352
                    INDEX 01 00:00:00
                """.trimIndent(),
            )
        }
        val resolved = StandaloneRomPaths.resolve(
            romPath = cue.absolutePath,
            romPathsJson = """["Virtua Cop/Virtua Cop.cue","Virtua Cop/Virtua Cop.bin"]""",
            romsTreeUri = null,
            romsDirPath = root.absolutePath,
            systemFolder = "ps2",
        )
        assertEquals(bin.absolutePath, resolved.romExtra)
        assertTrue(DiscDescriptorPaths.cueCompanionsReadable(cue))
    }

    @Test
    fun documentUriForRelativePath_buildsCompanionBinUri() {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        val uri = StandaloneRomPaths.documentUriForRelativePath(
            tree,
            "primary:ROMs",
            "ps2/Virtua Cop/Virtua Cop.bin",
        )
        assertNotNull(uri)
        assertTrue(uri!!.contains("Virtua%20Cop.bin", ignoreCase = true))
    }

    @Test
    fun selectBootContentUri_prefersSingleBinOverCue() {
        val cue =
            "content://host/tree/primary%3AROMs/document/primary%3AROMs%2Fps2%2Fgame%2Fgame.cue"
        val bin =
            "content://host/tree/primary%3AROMs/document/primary%3AROMs%2Fps2%2Fgame%2Fgame.bin"
        assertEquals(bin, StandaloneRomPaths.selectBootContentUri(cue, listOf(cue, bin)))
    }

    @Test
    fun selectBootContentUri_keepsIso() {
        val iso =
            "content://host/tree/primary%3AROMs/document/primary%3AROMs%2Fps2%2Fgame.iso"
        assertEquals(iso, StandaloneRomPaths.selectBootContentUri(iso, listOf(iso)))
    }
}

class EmulatorLauncherProfilesTest {

    @Test
    fun duckStationUsesMainWithBootPathExtra() {
        val profile = EmulatorLauncher.SUPPORTED_PROFILES.first { it.key == "DUCKSTATION" }
        assertEquals(StandaloneEmulatorProfile.LaunchMode.MAIN_WITH_EXTRAS, profile.launchMode)
        assertTrue(profile.pathExtraKeys.contains("bootPath"))
        assertEquals(false, profile.booleanExtras["resumeState"])
    }

    @Test
    fun aetherSx2UsesMainWithBootPathExtra() {
        val profile = EmulatorLauncher.SUPPORTED_PROFILES.first { it.key == "AETHERSX2" }
        assertEquals(StandaloneEmulatorProfile.LaunchMode.MAIN_WITH_EXTRAS, profile.launchMode)
        assertTrue(profile.pathExtraKeys.contains("bootPath"))
    }

    @Test
    fun melonDsUsesLaunchRomActionAndUriExtra() {
        val profile = EmulatorLauncher.SUPPORTED_PROFILES.first { it.key == "MELONDS" }
        assertEquals("me.magnum.melonds.LAUNCH_ROM", profile.intentAction)
        assertEquals(listOf("uri"), profile.pathExtraKeys)
    }

    @Test
    fun dolphinUsesAutoStartFileAndLeanback() {
        val profile = EmulatorLauncher.SUPPORTED_PROFILES.first { it.key == "DOLPHIN" }
        assertEquals(listOf("AutoStartFile"), profile.pathExtraKeys)
        assertTrue(profile.categories.contains(android.content.Intent.CATEGORY_LEANBACK_LAUNCHER))
    }

    @Test
    fun ppssppUsesViewWithData() {
        val profile = EmulatorLauncher.SUPPORTED_PROFILES.first { it.key == "PPSSPP" }
        assertEquals(StandaloneEmulatorProfile.LaunchMode.VIEW_URI, profile.launchMode)
        assertTrue(profile.putPathAsData)
    }
}
