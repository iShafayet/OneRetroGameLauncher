package com.sayemshafayet.onereogamelauncher.ra

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps ORGL / ES-DE system folder names to RetroAchievements console IDs.
 * IDs match rcheevos [RC_CONSOLE_*](https://github.com/RetroAchievements/rcheevos/blob/master/include/rc_consoles.h).
 */
@Singleton
class RaConsoleMapper @Inject constructor() {

    fun consoleIdForFolder(folderName: String?): Int? {
        if (folderName.isNullOrBlank()) return null
        val key = folderName.lowercase().trim()
        return FOLDER_TO_CONSOLE[key]
            ?: FOLDER_TO_CONSOLE.entries.firstOrNull { (alias, _) ->
                key == alias || key.contains(alias)
            }?.value
    }

    fun consoleName(consoleId: Int): String? = CONSOLE_NAMES[consoleId]

    companion object {
        private val FOLDER_TO_CONSOLE = mapOf(
            "genesis" to 1,
            "megadrive" to 1,
            "md" to 1,
            "n64" to 2,
            "snes" to 3,
            "sfc" to 3,
            "supernes" to 3,
            "supernintendo" to 3,
            "super nintendo" to 3,
            "super famicom" to 3,
            "superfamicom" to 3,
            "gb" to 4,
            "gameboy" to 4,
            "gba" to 5,
            "gbc" to 6,
            "gameboycolor" to 6,
            "nes" to 7,
            "famicom" to 7,
            "fc" to 7,
            "pce" to 8,
            "pcengine" to 8,
            "tg16" to 8,
            "turbografx" to 8,
            "segacd" to 9,
            "mcd" to 9,
            "32x" to 10,
            "mastersystem" to 11,
            "sms" to 11,
            "psx" to 12,
            "ps1" to 12,
            "playstation" to 12,
            "lynx" to 13,
            "atarilynx" to 13,
            "ngp" to 14,
            "neogeopocket" to 14,
            "gamegear" to 15,
            "gg" to 15,
            "gc" to 16,
            "gamecube" to 16,
            "jaguar" to 17,
            "atari5200" to 50,
            "5200" to 50,
            "nds" to 18,
            "ds" to 18,
            "wii" to 19,
            "ps2" to 21,
            "atari2600" to 25,
            "2600" to 25,
            "virtualboy" to 28,
            "vb" to 28,
            "msx" to 29,
            "msx2" to 29,
            "c64" to 30,
            "commodore64" to 30,
            "sg1000" to 33,
            "saturn" to 39,
            "dreamcast" to 40,
            "dc" to 40,
            "psp" to 41,
            "3do" to 43,
            "colecovision" to 44,
            "coleco" to 44,
            "intellivision" to 45,
            "vectrex" to 46,
            "pcfx" to 49,
            "atari7800" to 51,
            "7800" to 51,
            "wonderswan" to 53,
            "ws" to 53,
            "wsc" to 53,
            "neogeo_cd" to 56,
            "neogeocd" to 56,
            "fds" to 81,
            "fbneo" to 27,
            "arcade" to 27,
            "mame" to 27,
        )

        private val CONSOLE_NAMES = mapOf(
            1 to "Mega Drive",
            2 to "Nintendo 64",
            3 to "SNES",
            4 to "Game Boy",
            5 to "Game Boy Advance",
            6 to "Game Boy Color",
            7 to "NES",
            8 to "PC Engine",
            12 to "PlayStation",
            18 to "Nintendo DS",
            21 to "PlayStation 2",
            27 to "Arcade",
        )
    }
}
