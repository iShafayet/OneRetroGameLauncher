package com.sayemshafayet.onereogamelauncher.ui.util

import com.sayemshafayet.onereogamelauncher.data.db.entity.GameEntity

fun GameEntity.combinedLastPlayed(): Long? =
    listOfNotNull(esdeLastPlayed, orglLastPlayed).maxOrNull()

fun GameEntity.combinedLaunchCount(): Int = esdePlaycount + orglPlaycount

/** ORGL playtime from direct tracking plus Play-mode commitment sessions. */
fun GameEntity.totalOrglPlaytimeMs(commitmentSessionMs: Long = 0L): Long =
    orglPlaytimeMs + commitmentSessionMs

fun formatActivityLabel(playtimeMs: Long, launchCount: Int): String {
    val parts = buildList {
        if (playtimeMs > 0) add(formatDurationMs(playtimeMs))
        if (launchCount > 0) add("$launchCount launches")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Not tracked"
}
