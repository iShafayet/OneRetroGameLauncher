package com.sayemshafayet.onereogamelauncher.data.homebrew

import com.sayemshafayet.onereogamelauncher.domain.FreeHomebrewGame

data class LegalRomsGuideLink(
    val title: String,
    val url: String,
)

/**
 * Curated free (gratis + libre / redistributable) games ORGL may download for beginners.
 * Expand this list carefully — only titles we are allowed to redistribute.
 */
object FreeHomebrewCatalog {
    val LEGAL_ROMS_GUIDES: List<LegalRomsGuideLink> = listOf(
        LegalRomsGuideLink(
            title = "Steam guide: getting ROMs legally from collections",
            url = "https://steamcommunity.com/sharedfiles/filedetails/?id=2997730346",
        ),
        LegalRomsGuideLink(
            title = "Recalbox wiki: where to find 100% legal ROMs",
            url = "https://wiki.recalbox.com/en/tutorials/games/generalities/where-to-find-100-legal-roms",
        ),
        LegalRomsGuideLink(
            title = "RetroTechLab: legal ways to emulate retro games",
            url = "https://www.retrotechlab.com/legal-ways-to-emulate-retro-games-a-complete-guide/",
        ),
    )

    val games: List<FreeHomebrewGame> = listOf(
        FreeHomebrewGame(
            id = "apotris",
            title = "Apotris",
            systemFolder = "gba",
            fileName = "Apotris.gba",
            downloadUrl = "https://akouzoukos.com/files/apotris.gba",
            homepageUrl = "https://akouzoukos.com/apotris/downloads",
            blurb = "A legal, free & open-source block-stacking game for Game Boy Advance.",
        ),
    )
}
