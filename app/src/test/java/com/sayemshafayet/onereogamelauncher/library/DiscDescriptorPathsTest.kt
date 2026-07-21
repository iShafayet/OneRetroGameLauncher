package com.sayemshafayet.onereogamelauncher.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiscDescriptorPathsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun companionPathsRelativeTo_resolvesBesideCue() {
        val text = """
            FILE "Virtua Cop - Elite Edition (Europe).bin" BINARY
              TRACK 01 MODE2/2352
                INDEX 01 00:00:00
        """.trimIndent()
        val companions = DiscDescriptorPaths.companionPathsRelativeTo(
            "Virtua Cop - Elite Edition (Europe)/Virtua Cop - Elite Edition (Europe).cue",
            text,
        )
        assertEquals(
            listOf(
                "Virtua Cop - Elite Edition (Europe)/Virtua Cop - Elite Edition (Europe).bin",
            ),
            companions,
        )
    }

    @Test
    fun cueCompanionsReadable_requiresExistingBin() {
        val dir = tempFolder.newFolder("cue-dir")
        val bin = File(dir, "game.bin").apply { writeBytes(byteArrayOf(0)) }
        val cue = File(dir, "game.cue").apply {
            writeText("FILE \"${bin.name}\" BINARY\nTRACK 01 MODE2/2352\nINDEX 01 00:00:00\n")
        }
        assertTrue(DiscDescriptorPaths.cueCompanionsReadable(cue))
    }

    @Test
    fun realVirtuaCopCueCompanions_whenPresent() {
        val cue = File(
            "/mnt/linuxltsstorage/@WIP/ROMs (Ayaneo Only)/ps2/Virtua Cop - Elite Edition (Europe)/" +
                "Virtua Cop - Elite Edition (Europe).cue",
        )
        if (!cue.isFile) return
        assertTrue(DiscDescriptorPaths.cueCompanionsReadable(cue))
    }
}
