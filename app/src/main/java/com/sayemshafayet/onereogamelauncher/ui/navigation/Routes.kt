package com.sayemshafayet.onereogamelauncher.ui.navigation

import androidx.navigation.NavBackStackEntry
import com.sayemshafayet.onereogamelauncher.data.db.entity.CommitmentEntity

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"

    const val SETUP_LIBRARY = "setup/library"
    const val SETUP_LIBRARY_SEARCH = "setup/library/search"
    const val SETUP_ABOUT = "setup/about"
    const val SETUP_SYSTEM = "setup/system/{systemId}"
    const val SETUP_SYSTEM_EMULATOR = "setup/system/{systemId}/emulator"
    const val SETUP_GAME = "setup/game/{gameId}"
    const val SETUP_GAME_RA = "setup/game/{gameId}/retroachievements"
    const val SETUP_SETTINGS = "setup/settings"
    const val SETUP_SETTINGS_FOLDERS = "setup/settings/folders"
    const val SETUP_LIBRARY_SCAN = "setup/library/scan"
    const val SETUP_SETTINGS_ESDE = "setup/settings/esde"
    const val SETUP_SETTINGS_SCREENSCRAPER = "setup/settings/screenscraper"
    const val SETUP_SETTINGS_RA = "setup/settings/retroachievements"
    const val SETUP_SETTINGS_HLTB = "setup/settings/hltb"
    const val SETUP_SETTINGS_RETROARCH = "setup/settings/retroarch"
    const val SETUP_SETTINGS_PLAY_SLOTS = "setup/settings/play-slots"
    const val SETUP_SETTINGS_DATABASE = "setup/settings/database"
    const val SETUP_SETTINGS_CREDITS = "setup/settings/credits"
    const val SETUP_HISTORY = "setup/history"
    const val SETUP_HISTORY_RUN = "setup/history/run/{commitmentId}"
    const val SETUP_SCRAPE_WIZARD = "setup/scrape/wizard"

    const val PLAY_HOME = "play/home"
    const val PLAY_PICKER = "play/picker/{slotIndex}"
    const val PLAY_FOCUS = "play/focus/{slotIndex}"
    const val PLAY_COMMIT = "play/commit/{gameId}/{slotIndex}"
    const val PLAY_COMPLETE = "play/complete/{slotIndex}"

    fun setupSystem(systemId: Long) = "setup/system/$systemId"
    fun setupSystemEmulator(systemId: Long) = "setup/system/$systemId/emulator"
    fun setupGame(gameId: Long) = "setup/game/$gameId"
    fun gameRetroAchievements(gameId: Long) = "setup/game/$gameId/retroachievements"
    fun playPicker(slotIndex: Int = 0) = "play/picker/$slotIndex"
    fun playFocus(slotIndex: Int = 0) = "play/focus/$slotIndex"
    fun playCommit(gameId: Long, slotIndex: Int = 0) = "play/commit/$gameId/$slotIndex"
    fun playComplete(slotIndex: Int = 0) = "play/complete/$slotIndex"
    fun historyRun(commitmentId: Long) = "setup/history/run/$commitmentId"

    fun playHubForSlot(slotIndex: Int, occupied: Boolean): String =
        if (occupied) playFocus(slotIndex) else playPicker(slotIndex)

    /** Best hub to open when entering Play mode from cold start or Setup. */
    fun playEntryHub(activeCommitments: List<CommitmentEntity>): String {
        if (activeCommitments.isEmpty()) return playPicker(0)
        val slot = activeCommitments.minByOrNull { it.slotIndex }?.slotIndex ?: 0
        return playHubForSlot(slot, occupied = true)
    }

    fun slotIndexFromEntry(entry: NavBackStackEntry?): Int {
        if (entry == null) return 0
        val args = entry.arguments ?: return 0
        if (!args.containsKey("slotIndex")) return 0
        return args.getInt("slotIndex")
    }

    fun parsePlaySlotIndex(route: String?): Int? {
        if (route == null) return null
        val patterns = listOf(
            Regex("play/picker/(\\d+)"),
            Regex("play/focus/(\\d+)"),
            Regex("play/commit/\\d+/(\\d+)"),
            Regex("play/complete/(\\d+)"),
        )
        for (pattern in patterns) {
            pattern.matchEntire(route)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        }
        return null
    }

    fun isPlayHubRoute(route: String?): Boolean =
        route == PLAY_PICKER || route == PLAY_FOCUS
}
