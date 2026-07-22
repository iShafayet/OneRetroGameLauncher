package com.sayemshafayet.onereogamelauncher.data.orgl

import com.sayemshafayet.onereogamelauncher.domain.CommitmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrglRaCredentialsFileTest {
    @Test
    fun roundTrip_encryptsSecrets() {
        val encoded = OrglRaCredentialsFile.encode(
            OrglRaCredentialsFile.Credentials("player", "secret", "tok"),
        )
        assertTrue(encoded.contains("passwordEnc"))
        assertTrue(!encoded.contains("\"secret\""))
        assertTrue(!encoded.contains("\"tok\""))
        val decoded = OrglRaCredentialsFile.decode(encoded)
        assertNotNull(decoded)
        assertEquals("player", decoded!!.user)
        assertEquals("secret", decoded.password)
        assertEquals("tok", decoded.token)
    }

    @Test
    fun decodesLegacyPlaintextV1() {
        val legacy = """{"specVersion":1,"user":"player","password":"secret","token":"tok"}"""
        val decoded = OrglRaCredentialsFile.decode(legacy)
        assertNotNull(decoded)
        assertEquals("secret", decoded!!.password)
        assertEquals("tok", decoded.token)
    }

    @Test
    fun rejectsBlank() {
        assertNull(OrglRaCredentialsFile.decode("""{"specVersion":1,"user":"","password":"x"}"""))
    }
}

class OrglPlayHistoryFileTest {
    @Test
    fun roundTripJournalEntry() {
        val snapshot = OrglPlayHistoryFile.Snapshot(
            commitments = listOf(
                OrglPlayHistoryFile.Commitment(
                    systemFolder = "nes",
                    fileName = "Mario.nes",
                    title = "Mario",
                    committedAt = 10L,
                    releasedAt = 20L,
                    status = CommitmentStatus.FINISHED,
                    review = OrglPlayHistoryFile.Review(4.5f, "great", 20L),
                    sessions = listOf(
                        OrglPlayHistoryFile.Session(10L, 20L, 10L),
                    ),
                ),
            ),
        )
        val encoded = OrglPlayHistoryFile.encode(snapshot)
        assertTrue(encoded.contains("\"specVersion\":${OrglPlayHistoryFile.SPEC_VERSION}"))
        assertTrue(!encoded.contains("\"games\""))
        val decoded = OrglPlayHistoryFile.decode(encoded)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.commitments.size)
        assertEquals(4.5f, decoded.commitments[0].review!!.stars, 0.01f)
        assertEquals("Mario", decoded.commitments[0].title)
    }

    @Test
    fun rejectsLegacyV1WithGamesArray() {
        val legacy = """
            {"specVersion":1,"games":[{"systemFolder":"nes","fileName":"a.nes"}],"commitments":[]}
        """.trimIndent()
        assertNull(OrglPlayHistoryFile.decode(legacy))
    }

    @Test
    fun acceptsEmptyJournal() {
        val raw = """{"specVersion":2,"commitments":[]}"""
        val decoded = OrglPlayHistoryFile.decode(raw)
        assertNotNull(decoded)
        assertTrue(decoded!!.commitments.isEmpty())
    }
}

class OrglPlayHistoryMatchTest {
    @Test
    fun normalizeRomFileName_stripsPath() {
        assertEquals("game.zip", normalizeRomFileName("Set 2/game.zip"))
        assertEquals("game.zip", normalizeRomFileName("game.zip"))
    }

    @Test
    fun matchGameByBasename() {
        val systemId = 1L
        val games = listOf(
            com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity(
                id = 9,
                systemId = systemId,
                title = "Super Mario",
                romPath = "content://tree/nes/Mario.nes",
                fileName = "Mario.nes",
            ),
        )
        val matched = matchGameInList(
            fileName = "Set 2/Mario.nes",
            title = null,
            games = games,
        )
        assertEquals(9L, matched?.id)
    }
}
