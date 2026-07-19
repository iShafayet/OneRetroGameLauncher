package com.sayemshafayet.onereogamelauncher.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SafPathResolverTest {
    @Test
    fun percentDecode_decodesColonAndSlash() {
        assertEquals("primary:ROMs/Favorites", SafPathResolver.percentDecode("primary%3AROMs%2FFavorites"))
        assertEquals("ROMs (Favorites)", SafPathResolver.percentDecode("ROMs%20(Favorites)"))
    }

    @Test
    fun documentIdToFilesystemPath_primary() {
        assertEquals(
            "/storage/emulated/0/ROMs/Favorites",
            SafPathResolver.documentIdToFilesystemPath("primary%3AROMs%2FFavorites"),
        )
        assertEquals(
            "/storage/emulated/0/ROMs (Favorites)",
            SafPathResolver.documentIdToFilesystemPath("primary:ROMs (Favorites)"),
        )
    }

    @Test
    fun looksLikeRomsRoot_requiresKnownSystemFolder() {
        val dir = createTempDir("orgl-roms")
        try {
            assertFalse(SafPathResolver.looksLikeRomsRoot(dir, listOf("snes", "nes")))
            File(dir, "snes").mkdir()
            assertTrue(SafPathResolver.looksLikeRomsRoot(dir, listOf("snes", "nes")))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun displayLabel_prefersPathHint() {
        assertEquals(
            "/storage/emulated/0/ROMs",
            SafPathResolver.displayLabel(
                "content://com.android.externalstorage.documents/tree/primary%3AROMs",
                "/storage/emulated/0/ROMs",
            ),
        )
    }
}
