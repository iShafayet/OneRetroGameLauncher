package com.sayemshafayet.onereogamelauncher.play

import com.sayemshafayet.onereogamelauncher.data.db.dao.JournalEntryRow

fun JournalEntryRow.toPlayCompletionData(): PlayCompletionData =
    PlayCompletionData(
        collagePath = collagePath,
        gameTitle = gameTitle,
        systemName = systemDisplayName,
        status = status,
        stars = stars ?: 0f,
        playtimeMs = playtimeMs,
        sessionCount = sessionCount,
        reviewExcerpt = reviewText?.take(120),
    )
