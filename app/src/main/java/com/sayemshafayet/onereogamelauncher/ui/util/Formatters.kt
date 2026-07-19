package com.sayemshafayet.onereogamelauncher.ui.util

import java.util.concurrent.TimeUnit

fun formatDurationMs(ms: Long): String {
    if (ms <= 0) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

fun formatHours(hours: Double?): String {
    if (hours == null || hours <= 0) return "—"
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun starsLabel(stars: Float): String {
    val full = stars.toInt()
    val half = stars - full >= 0.5f
    return buildString {
        repeat(full) { append('★') }
        if (half) append('½')
        repeat(5 - full - if (half) 1 else 0) { append('☆') }
    }
}

fun formatDate(epochMs: Long?): String {
    if (epochMs == null) return "—"
    return java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        .format(java.util.Date(epochMs))
}
