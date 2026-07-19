package com.sayemshafayet.onereogamelauncher.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"

    const val SETUP_LIBRARY = "setup/library"
    const val SETUP_SYSTEM = "setup/system/{systemId}"
    const val SETUP_GAME = "setup/game/{gameId}"
    const val SETUP_SETTINGS = "setup/settings"
    const val SETUP_SETTINGS_ESDE = "setup/settings/esde"
    const val SETUP_SETTINGS_SCREENSCRAPER = "setup/settings/screenscraper"
    const val SETUP_SETTINGS_RA = "setup/settings/retroachievements"
    const val SETUP_SETTINGS_HLTB = "setup/settings/hltb"
    const val SETUP_SETTINGS_RETROARCH = "setup/settings/retroarch"
    const val SETUP_SETTINGS_CREDITS = "setup/settings/credits"
    const val SETUP_SCRAPE = "setup/scrape"

    const val PLAY_HOME = "play/home"
    const val PLAY_PICKER = "play/picker"
    const val PLAY_FOCUS = "play/focus"
    const val PLAY_JOURNAL = "play/journal"
    const val PLAY_COMMIT = "play/commit/{gameId}"

    fun setupSystem(systemId: Long) = "setup/system/$systemId"
    fun setupGame(gameId: Long) = "setup/game/$gameId"
    fun playCommit(gameId: Long) = "play/commit/$gameId"
}
