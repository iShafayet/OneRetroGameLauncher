package com.sayemshafayet.onereogamelauncher.domain

enum class RaVisualState {
    /** Game is not listed on RetroAchievements (no achievement set). */
    NO_GAME,
    /** RA account not configured in ORGL. */
    SIGN_IN_REQUIRED,
    /** Game has achievements but current ROM hash or emulator cannot earn them. */
    UNSUPPORTED_SETUP,
    /** Ready to view progress; launch via RetroArch with a matching ROM can earn achievements. */
    READY,
    /** Network or API failure — distinct from unsupported. */
    ERROR,
}

/** Outcome of an RA operation: success, unsupported game/ROM, or transient failure. */
sealed class RaResult<out T> {
    data class Ok<T>(val value: T) : RaResult<T>()
    data class Unsupported(val message: String) : RaResult<Nothing>()
    data class Failed(val message: String) : RaResult<Nothing>()
}

data class RaButtonState(
    val visual: RaVisualState,
    val subtitle: String,
    val enabled: Boolean,
)

data class RaLookupResult(
    val gameId: Int,
    val title: String?,
    val consoleName: String?,
    val totalAchievements: Int,
    val romHash: String?,
    /** True when the ROM MD5 is recognized by RetroAchievements. */
    val hashRecognized: Boolean,
)

data class RaAchievement(
    val id: Int,
    val title: String,
    val description: String,
    val points: Int,
    val badgeUrl: String?,
    val earned: Boolean,
    val earnedHardcore: Boolean,
    val earnedAt: String?,
)

data class RaGameDetails(
    val gameId: Int,
    val title: String,
    val consoleName: String?,
    val earned: Int,
    val total: Int,
    val hardcoreEarned: Int,
    val pointsEarned: Int,
    val pointsTotal: Int,
    val achievements: List<RaAchievement>,
    val recentUnlocks: List<String>,
)
