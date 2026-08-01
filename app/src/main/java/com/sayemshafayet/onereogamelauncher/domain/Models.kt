package com.sayemshafayet.onereogamelauncher.domain

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    /** Always-dark arcade neon; ignores system light/dark. */
    NEON,
    /** Soft peaceful sage/mist stage; ignores system light/dark. */
    CALM,
    ;

    val label: String
        get() = when (this) {
            SYSTEM -> "System"
            LIGHT -> "Light"
            DARK -> "Dark"
            NEON -> "Neon"
            CALM -> "Calm"
        }
}

enum class AppMode { SETUP, PLAY }

enum class CommitmentStatus { ACTIVE, FINISHED, DROPPED }

enum class MediaType {
    BOX_2D, BOX_3D, SCREENSHOT, TITLE, MARQUEE, VIDEO, FANART, UNKNOWN;

    companion object {
        fun fromEsDeFolder(name: String): MediaType = when (name.lowercase()) {
            "covers", "box2d", "boxfront" -> BOX_2D
            "3dboxes", "box3d" -> BOX_3D
            "screenshots", "miximages" -> SCREENSHOT
            "titlescreens", "titles" -> TITLE
            "marquees", "logos" -> MARQUEE
            "videos" -> VIDEO
            "fanart" -> FANART
            else -> UNKNOWN
        }
    }
}

enum class LaunchSeverity { OK, WARN, ERROR }

data class LaunchCheck(
    val id: String,
    val label: String,
    val severity: LaunchSeverity,
    val detail: String,
    val fixGuidance: String? = null,
)

data class SystemDef(
    val name: String,
    val fullName: String,
    val folder: String,
    val extensions: Set<String>,
    val platform: String,
    val commands: List<SystemCommand>,
)

data class SystemCommand(
    val label: String,
    val template: String,
)

data class ScrapeProgress(
    val currentTitle: String,
    val index: Int,
    val total: Int,
)

enum class ScrapeGameFilter {
    ALL,
    MISSING_METADATA,
    MISSING_ANY_MEDIA,
    MISSING_VIDEO,
}

data class ScrapeSessionState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val cancelled: Boolean = false,
    val currentTitle: String = "",
    val currentSystem: String = "",
    val index: Int = 0,
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val pending: Int = 0,
    val startedAtMs: Long? = null,
    val finishedAtMs: Long? = null,
    val lastError: String? = null,
) {
    val elapsedMs: Long
        get() {
            val start = startedAtMs ?: return 0L
            val end = finishedAtMs ?: System.currentTimeMillis()
            return (end - start).coerceAtLeast(0L)
        }
}

enum class ScanStage {
    PREPARING,
    SCANNING_SYSTEM,
}

data class ScanProgress(
    val stage: ScanStage = ScanStage.PREPARING,
    val statusMessage: String = "",
    val systemName: String = "",
    val systemFolder: String = "",
    /** Games found in the system currently being processed. */
    val gamesInCurrentSystem: Int = 0,
    /** Games processed so far in the current system (for intra-system progress). */
    val gamesProcessedInSystem: Int = 0,
    /** Cumulative games across all systems processed so far. */
    val gamesTotal: Int = 0,
    /** Cumulative media files linked so far. */
    val mediaTotal: Int = 0,
    /** Cumulative unrecognized files so far. */
    val unknownFiles: Int = 0,
    val systemsDone: Int = 0,
    val systemsTotal: Int = 0,
) {
    val progressFraction: Float
        get() {
            if (systemsTotal <= 0) return 0f
            val systemFraction = systemsDone.toFloat() / systemsTotal
            val intra = if (gamesInCurrentSystem > 0) {
                gamesProcessedInSystem.toFloat() / gamesInCurrentSystem / systemsTotal
            } else {
                0f
            }
            return (systemFraction + intra).coerceIn(0f, 1f)
        }
}

data class HltbEstimate(
    val gameId: Long?,
    val title: String,
    val mainHours: Double?,
    val mainExtraHours: Double?,
    val completionistHours: Double?,
)

/** Compact HLTB column display phase (loading / value / empty / transport error). */
enum class HltbUiPhase {
    Loading,
    Ready,
    Missing,
    Error,
}

data class RaProgress(
    val gameId: Int?,
    val title: String?,
    val earned: Int,
    val total: Int,
    val softcoreEarned: Int = 0,
    val recentUnlocks: List<String> = emptyList(),
)

/** Per-system row for post-scan library breakdown. */
data class SystemScanSummary(
    val systemId: Long,
    val displayName: String,
    val folderName: String,
    val gameCount: Int,
    /** Games with imported metadata (description, developer, genre, etc.). */
    val withMetadata: Int,
    /** Games that have at least one linked media file. */
    val withMedia: Int,
    /** Total media rows linked for games in this system. */
    val mediaFiles: Int,
)

/** Aggregate + per-system breakdown after a library scan. */
data class LibraryScanSummary(
    val systemsWithGames: Int,
    val gamesFound: Int,
    val gamesWithMetadata: Int,
    val gamesWithMedia: Int,
    val mediaLinked: Int,
    val unknownFiles: Int = 0,
    val systems: List<SystemScanSummary> = emptyList(),
)
