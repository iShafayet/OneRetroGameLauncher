package com.sayemshafayet.onereogamelauncher.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class GamelistParserTest {

    private val parser = GamelistParser()

    @Test
    fun parse_readsDescAndDescriptionTags() {
        val xml = """
            <?xml version="1.0"?>
            <gameList>
              <game>
                <path>./Game.nes</path>
                <name>Super Game</name>
                <description>A great game.</description>
                <developer>Nintendo</developer>
                <genre>Platform</genre>
              </game>
            </gameList>
        """.trimIndent()
        val entries = parser.parse(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(1, entries.size)
        assertEquals("Game.nes", entries[0].path)
        assertEquals("Super Game", entries[0].name)
        assertEquals("A great game.", entries[0].desc)
        assertEquals("Nintendo", entries[0].developer)
        assertEquals("Platform", entries[0].genre)
    }

    @Test
    fun normalizePath_stripsDotSlashPrefix() {
        val xml = """
            <gameList>
              <game>
                <path>./subdir/Title.zip</path>
                <name>Clean Title</name>
              </game>
            </gameList>
        """.trimIndent()
        val entry = parser.parse(ByteArrayInputStream(xml.toByteArray())).single()
        assertEquals("subdir/Title.zip", entry.path)
    }
}

class GamelistSourcesTest {

    @Test
    fun merge_esdeOverridesRomFolder() {
        val esde = mapOf("a.nes" to sampleEntry("a.nes", "ES-DE Name"))
        val rom = mapOf("a.nes" to sampleEntry("a.nes", "ROM Name"))
        val merged = GamelistSources.merge(esde, rom)
        assertEquals("ES-DE Name", merged["a.nes"]?.name)
    }

    @Test
    fun merge_keepsRomOnlyEntries() {
        val esde = mapOf("a.nes" to sampleEntry("a.nes", "A"))
        val rom = mapOf(
            "a.nes" to sampleEntry("a.nes", "A-old"),
            "b.nes" to sampleEntry("b.nes", "B"),
        )
        val merged = GamelistSources.merge(esde, rom)
        assertEquals(2, merged.size)
        assertEquals("B", merged["b.nes"]?.name)
    }

    private fun sampleEntry(path: String, name: String) = GamelistEntry(
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
