package com.sayemshafayet.onereogamelauncher.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RomScanLogicTest {

    @Test
    fun isPlayableRom_acceptsCueEvenWhenNotInSystemExtensions() {
        assertTrue(RomScanLogic.isPlayableRom("cue", setOf("bin", "iso")))
        assertTrue(RomScanLogic.isPlayableRom("gdi", setOf("bin")))
        assertFalse(RomScanLogic.isPlayableRom("txt", setOf("sfc", "zip")))
    }

    @Test
    fun gamelistMatch_findsEntryInDifferentSubfolderByFilename() {
        val gamelist = mapOf(
            "Set 2 - RA/Super Mario World (USA).zip" to entry(
                path = "Set 2 - RA/Super Mario World (USA).zip",
                name = "Super Mario World",
            ),
        )
        val match = RomScanLogic.gamelistMatch(
            gamelist,
            relativePath = "TestSub/Super Mario World (USA).zip",
            fileName = "Super Mario World (USA).zip",
        )
        assertEquals("Super Mario World", match?.name)
    }

    @Test
    fun gamelistMatch_prefersExactPathOverFilenameCollision() {
        val gamelist = mapOf(
            "Set A/Game.zip" to entry("Set A/Game.zip", "Set A Title"),
            "Set B/Game.zip" to entry("Set B/Game.zip", "Set B Title"),
        )
        val match = RomScanLogic.gamelistMatch(
            gamelist,
            relativePath = "Set B/Game.zip",
            fileName = "Game.zip",
        )
        assertEquals("Set B Title", match?.name)
    }

    @Test
    fun combineGamelistMediaPath_resolvesRelativeToGamelistEntryDirectory() {
        assertEquals(
            "Set 2 - RA/cover.png",
            RomScanLogic.combineGamelistMediaPath("Set 2 - RA", "./cover.png"),
        )
    }

    @Test
    fun combineGamelistMediaPath_skipsDownloadedMediaPaths() {
        assertNull(
            RomScanLogic.combineGamelistMediaPath(
                "Set 2 - RA",
                "./downloaded_media/snes/covers/Game.png",
            ),
        )
    }

    private fun entry(path: String, name: String) = GamelistEntry(
        path = path,
        name = name,
        desc = null,
        rating = null,
        releasedate = null,
        developer = null,
        publisher = null,
        genre = null,
        players = null,
        playcount = null,
        lastplayed = null,
        favorite = null,
        image = null,
        video = null,
        marquee = null,
        thumbnail = null,
    )
}
