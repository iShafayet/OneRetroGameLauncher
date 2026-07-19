package com.sayemshafayet.onereogamelauncher.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroArchRomPathsTest {
    @Test
    fun encodeTreeForSaf_encodesSlashAndPercent() {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        val encoded = RetroArchRomPaths.encodeTreeForSaf(tree)
        assertEquals(
            "content:%2F%2Fcom.android.externalstorage.documents%2Ftree%2Fprimary%253AROMs",
            encoded,
        )
    }

    @Test
    fun buildSafPath_joinsRelative() {
        val tree = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        val saf = RetroArchRomPaths.buildSafPath(tree, "snes/Super Mario World.sfc")
        assertTrue(saf.startsWith("saf://"))
        assertTrue(saf.endsWith("/snes/Super Mario World.sfc"))
        assertTrue(saf.contains("%2Ftree%2F"))
    }

    @Test
    fun relativeFromContentUri_extractsPathUnderTree() {
        val uri =
            "content://com.android.externalstorage.documents/tree/primary%3AROMs/" +
                "document/primary%3AROMs%2Fsnes%2FGame.sfc"
        assertEquals("snes/Game.sfc", RetroArchRomPaths.relativeFromContentUri(uri))
    }

    @Test
    fun resolve_prefersSafFromTreeAndRelative() {
        val resolved = RetroArchRomPaths.resolve(
            romPath = "content://com.android.externalstorage.documents/tree/primary%3AROMs/" +
                "document/primary%3AROMs%2Fsnes%2FGame.sfc",
            romPathsJson = """["Game.sfc"]""",
            systemFolder = "snes",
            romsTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AROMs",
            romsDirPath = "/storage/emulated/0/ROMs",
        )
        assertTrue(resolved.romExtra.startsWith("saf://"))
        assertTrue(resolved.romExtra.endsWith("/snes/Game.sfc"))
        assertNotNull(resolved.grantTreeUri)
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3AROMs",
            resolved.grantTreeUri,
        )
    }

    @Test
    fun contentUriToSaf_buildsSafPath() {
        val uri =
            "content://com.android.externalstorage.documents/tree/primary%3AROMs/" +
                "document/primary%3AROMs%2Fnes%2FMario.nes"
        val saf = RetroArchRomPaths.contentUriToSaf(uri)
        assertNotNull(saf)
        assertTrue(saf!!.romExtra.startsWith("saf://"))
        assertTrue(saf.romExtra.endsWith("/nes/Mario.nes"))
    }
}
