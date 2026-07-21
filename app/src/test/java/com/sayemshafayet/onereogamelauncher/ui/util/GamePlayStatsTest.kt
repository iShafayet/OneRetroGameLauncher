package com.sayemshafayet.onereogamelauncher.ui.util

import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class GamePlayStatsTest {

    @Test
    fun combinedLastPlayed_picksLatest() {
        val game = sampleGame(esdeLastPlayed = 1000L, orglLastPlayed = 2000L)
        assertEquals(2000L, game.combinedLastPlayed())
    }

    @Test
    fun combinedLaunchCount_sumsEsdeAndOrgl() {
        val game = sampleGame(esdePlaycount = 3, orglPlaycount = 2)
        assertEquals(5, game.combinedLaunchCount())
    }

    @Test
    fun formatActivityLabel_includesPlaytimeAndLaunches() {
        assertEquals("1h 0m · 5 launches", formatActivityLabel(3_600_000L, 5))
    }

    private fun sampleGame(
        esdePlaycount: Int = 0,
        orglPlaycount: Int = 0,
        esdeLastPlayed: Long? = null,
        orglLastPlayed: Long? = null,
    ) = GameEntity(
        systemId = 1L,
        title = "Test",
        romPath = "/rom",
        fileName = "test.nes",
        esdePlaycount = esdePlaycount,
        orglPlaycount = orglPlaycount,
        esdeLastPlayed = esdeLastPlayed,
        orglLastPlayed = orglLastPlayed,
    )
}
